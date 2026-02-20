package com.prreview.app.service;

import com.prreview.app.enums.Role;
import com.prreview.app.model.AuditLog;
import com.prreview.app.model.PullRequest;
import com.prreview.app.model.User;
import com.prreview.app.repository.AuditLogRepository;
import com.prreview.app.repository.PullRequestRepository;
import com.prreview.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SmartAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(SmartAssignmentService.class);

    private final UserRepository userRepository;
    private final PullRequestRepository pullRequestRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;

    /**
     * Suggests potential reviewers for a pull request based on expertise, history, and load.
     */
    public List<User> suggestReviewers(PullRequest pr, int maxReviewers) {
        if (pr == null || pr.getAuthor() == null) {
            return List.of();
        }

        Set<String> keywords = extractKeywords(pr);
        if (keywords.isEmpty()) {
            // Fallback: just return reviewers ordered by id (stable)
            return userRepository.findByRole(Role.REVIEWER)
                    .stream()
                    .sorted(Comparator.comparing(User::getId))
                    .limit(maxReviewers)
                    .toList();
        }

        // All reviewers
        List<User> reviewers = userRepository.findByRole(Role.REVIEWER);
        if (reviewers.isEmpty()) {
            return List.of();
        }

        // Load a reasonable slice of recent audit logs for scoring (e.g., last 500)
        List<AuditLog> recentLogs = auditLogRepository
                .findAllByOrderByTimestampDesc(org.springframework.data.domain.PageRequest.of(0, 500))
                .getContent();

        Map<User, Integer> scores = new HashMap<>();

        for (User reviewer : reviewers) {
            int score = 0;

            // +3 if expertiseTags contains any keyword
            if (matchesExpertise(reviewer, keywords)) {
                score += 3;
            }

            // +2 if reviewer has reviewed similar PR before (based on audit logs)
            if (hasReviewedSimilar(reviewer, keywords, recentLogs)) {
                score += 2;
            }

            // -5 if reviewer is overloaded for a weekly limit of 5
            if (reviewer.isOverloaded(5)) {
                score -= 5;
            }

            // -2 if reviewer frequently reviews same author
            if (frequentlyReviewsSameAuthor(reviewer, pr, recentLogs)) {
                score -= 2;
            }

            scores.put(reviewer, score);
        }

        return reviewers.stream()
                .sorted(Comparator.comparingInt((User u) -> scores.getOrDefault(u, 0)).reversed()
                        .thenComparing(User::getId))
                .limit(maxReviewers)
                .toList();
    }

    /**
     * Automatically assigns up to three balanced reviewers to the given PR.
     * This overload does not expose any UI model and only performs assignment and logging.
     */
    public void autoAssignBalancedReviewers(PullRequest pr) {
        autoAssignBalancedReviewers(pr, null);
    }

    /**
     * Automatically assigns up to three balanced reviewers to the given PR and optionally
     * collects knowledge-gap warnings into the provided model.
     */
    public void autoAssignBalancedReviewers(PullRequest pr, Model model) {
        if (pr == null || pr.getId() == null) {
            return;
        }

        PullRequest managedPr = pullRequestRepository.findById(pr.getId())
                .orElse(pr);

        List<User> suggested = suggestReviewers(managedPr, 3);

        // Filter out overloaded reviewers again to be safe
        List<User> balanced = suggested.stream()
                .filter(u -> !u.isOverloaded(5))
                .toList();

        if (balanced.isEmpty()) {
            log.debug("No suitable reviewers found for PR id={}", managedPr.getId());
            return;
        }

        // Assign reviewers
        Set<User> reviewersSet = Optional.ofNullable(managedPr.getReviewers())
                .map(HashSet::new)
                .orElseGet(HashSet::new);
        reviewersSet.addAll(balanced);
        managedPr.setReviewers(reviewersSet);

        // Increase weeklyReviewCount and ensure reset logic is applied
        List<String> knowledgeGapWarnings = new ArrayList<>();
        for (User reviewer : balanced) {
            // Calling isOverloaded will reset weeklyReviewCount if needed
            reviewer.isOverloaded(5);
            reviewer.setWeeklyReviewCount(
                    Optional.ofNullable(reviewer.getWeeklyReviewCount()).orElse(0) + 1
            );

            // Knowledge gap detection
            if (hasKnowledgeGap(reviewer, managedPr)) {
                String message = "Knowledge Gap Alert for " + reviewer.getName();
                knowledgeGapWarnings.add(message);
            }
        }

        // Update PR status to IN_REVIEW if not already
        if (managedPr.getStatus() != com.prreview.app.enums.PRStatus.IN_REVIEW) {
            managedPr.setStatus(com.prreview.app.enums.PRStatus.IN_REVIEW);
        }

        // Persist changes
        pullRequestRepository.save(managedPr);
        userRepository.saveAll(balanced);

        // Determine current user for logging
        User currentUser = resolveCurrentUser();
        if (currentUser == null) {
            currentUser = managedPr.getAuthor();
        }

        auditLogService.logAction(managedPr, "Smart Auto Assignment", currentUser);

        // If any knowledge-gap warnings were detected, log them and expose via model attribute
        if (!knowledgeGapWarnings.isEmpty()) {
            for (String msg : knowledgeGapWarnings) {
                auditLogService.logAction(managedPr, msg, currentUser);
            }
            if (model != null) {
                model.addAttribute("smartAssignmentWarnings", knowledgeGapWarnings);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    /**
     * Returns true if the given reviewer appears to have a knowledge gap for the given PR:
     * - no matching expertise tags, and
     * - has never reviewed PRs with similar keywords.
     */
    public boolean hasKnowledgeGap(User reviewer, PullRequest pr) {
        if (reviewer == null || pr == null) {
            return false;
        }

        Set<String> keywords = extractKeywords(pr);
        if (keywords.isEmpty()) {
            return false;
        }

        // If they have expertise that matches, it's not a gap
        if (matchesExpertise(reviewer, keywords)) {
            return false;
        }

        // Load recent logs to check for similar reviews
        List<AuditLog> recentLogs = auditLogRepository
                .findAllByOrderByTimestampDesc(org.springframework.data.domain.PageRequest.of(0, 500))
                .getContent();

        boolean hasReviewedSimilar = hasReviewedSimilar(reviewer, keywords, recentLogs);
        return !hasReviewedSimilar;
    }

    /**
     * Calculates the variance of weeklyReviewCount across all reviewers.
     * Returns 0.0 when there are no reviewers or only one reviewer.
     */
    public double calculateWorkloadVariance() {
        List<User> reviewers = userRepository.findByRole(Role.REVIEWER);
        if (reviewers == null || reviewers.size() <= 1) {
            return 0.0d;
        }

        // Treat null counts as 0
        List<Integer> counts = reviewers.stream()
                .map(u -> Optional.ofNullable(u.getWeeklyReviewCount()).orElse(0))
                .toList();

        double mean = counts.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0d);

        double variance = counts.stream()
                .mapToDouble(c -> {
                    double diff = c - mean;
                    return diff * diff;
                })
                .average()
                .orElse(0.0d);

        return variance;
    }

    private Set<String> extractKeywords(PullRequest pr) {
        Set<String> keywords = new HashSet<>();
        addKeywordsFromString(keywords, pr.getTitle());
        addKeywordsFromString(keywords, pr.getDescription());
        addKeywordsFromString(keywords, pr.getBranchName());
        return keywords;
    }

    private void addKeywordsFromString(Set<String> target, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String[] parts = text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        for (String p : parts) {
            if (!p.isBlank() && p.length() > 2) {
                target.add(p);
            }
        }
    }

    private boolean matchesExpertise(User reviewer, Set<String> keywords) {
        String tags = reviewer.getExpertiseTags();
        if (tags == null || tags.isBlank()) {
            return false;
        }
        Set<String> expertise = Arrays.stream(tags.split(","))
                .map(String::trim)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
        for (String kw : keywords) {
            if (expertise.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasReviewedSimilar(User reviewer, Set<String> keywords, List<AuditLog> recentLogs) {
        return recentLogs.stream()
                .filter(log -> AuditLogService.REVIEW_SUBMITTED.equals(log.getAction()))
                .filter(log -> reviewer.equals(log.getPerformedBy()))
                .map(AuditLog::getPullRequest)
                .filter(Objects::nonNull)
                .anyMatch(pr -> overlapsWithKeywords(pr, keywords));
    }

    private boolean frequentlyReviewsSameAuthor(User reviewer, PullRequest targetPr, List<AuditLog> recentLogs) {
        if (targetPr.getAuthor() == null) {
            return false;
        }
        long count = recentLogs.stream()
                .filter(log -> AuditLogService.REVIEW_SUBMITTED.equals(log.getAction()))
                .filter(log -> reviewer.equals(log.getPerformedBy()))
                .map(AuditLog::getPullRequest)
                .filter(Objects::nonNull)
                .filter(pr -> targetPr.getAuthor().equals(pr.getAuthor()))
                .count();

        // Arbitrary threshold: more than 3 recent reviews for this author counts as "frequent"
        return count > 3;
    }

    private boolean overlapsWithKeywords(PullRequest pr, Set<String> keywords) {
        Set<String> prKeywords = extractKeywords(pr);
        prKeywords.retainAll(keywords);
        return !prKeywords.isEmpty();
    }

    private User resolveCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return null;
            }
            Object principal = auth.getPrincipal();
            if (principal instanceof User user) {
                return user;
            }
        } catch (Exception e) {
            log.debug("Could not resolve current user for SmartAutoAssignment logging: {}", e.getMessage());
        }
        return null;
    }
}

