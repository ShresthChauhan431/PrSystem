package com.prreview.app.service;

import com.prreview.app.dto.PullRequestDTO;
import com.prreview.app.enums.PRStatus;
import com.prreview.app.exception.InvalidStateTransitionException;
import com.prreview.app.model.PullRequest;
import com.prreview.app.model.Review;
import com.prreview.app.model.User;
import com.prreview.app.repository.PullRequestRepository;
import com.prreview.app.repository.ReviewRepository;
import com.prreview.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PullRequestService {

    private final PullRequestRepository pullRequestRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public List<PullRequest> findPullRequestsForUser(User user) {
        if (user.getRole() == com.prreview.app.enums.Role.ADMIN
                || user.getRole() == com.prreview.app.enums.Role.TEAM_LEAD) {
            return pullRequestRepository.findAll(org.springframework.data.domain.Sort
                    .by(org.springframework.data.domain.Sort.Direction.DESC, "updatedAt"));
        }
        return pullRequestRepository.findByAuthorOrReviewersContaining(user, user);
    }

    public Optional<PullRequest> findById(Long id) {
        return pullRequestRepository.findById(id);
    }

    @Transactional
    public PullRequest createPullRequest(PullRequestDTO dto, User author) {
        PullRequest pr = PullRequest.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .branchName(dto.getBranchName())
                .repoUrl(dto.getRepoUrl())
                .status(PRStatus.OPEN)
                .author(author)
                .reviewers(Set.of())
                .build();
        pr = pullRequestRepository.save(pr);
        try {
            notificationService.sendPRCreatedNotification(pr);
        } catch (Exception e) {
            log.warn("Failed to send PR created notifications: {}", e.getMessage());
        }
        return pr;
    }

    @Transactional
    public PullRequest updatePullRequest(Long id, PullRequestDTO dto, User currentUser) {
        PullRequest pr = pullRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pull request not found"));
        if (pr.getStatus() != PRStatus.OPEN) {
            throw new IllegalArgumentException("Only OPEN pull requests can be edited");
        }
        pr.setTitle(dto.getTitle());
        pr.setDescription(dto.getDescription());
        pr.setBranchName(dto.getBranchName());
        pr.setRepoUrl(dto.getRepoUrl());
        pr = pullRequestRepository.save(pr);
        return pr;
    }

    @Transactional
    public PullRequest assignReviewers(Long id, List<Long> reviewerIds, User currentUser) {
        PullRequest pr = pullRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pull request not found"));
        if (pr.getStatus() != PRStatus.OPEN) {
            throw new InvalidStateTransitionException(
                    "Cannot assign reviewers. Pull request must be in OPEN status, but it is currently "
                            + pr.getStatus().name());
        }
        Set<User> reviewers = reviewerIds == null ? Set.of()
                : reviewerIds.stream()
                        .map(userRepository::findById)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet());
        pr.setReviewers(reviewers);
        pr.setStatus(PRStatus.IN_REVIEW);
        pr = pullRequestRepository.save(pr);
        try {
            notificationService.sendReviewerAssignedNotification(pr, new ArrayList<>(reviewers));
        } catch (Exception e) {
            log.warn("Failed to send reviewer assigned notifications: {}", e.getMessage());
        }
        return pr;
    }

    @Transactional
    public PullRequest approve(Long id, User currentUser) {
        PullRequest pr = pullRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pull request not found"));
        if (pr.getStatus() != PRStatus.IN_REVIEW) {
            throw new InvalidStateTransitionException(
                    "Cannot approve pull request. It must be in IN_REVIEW status, but it is currently "
                            + pr.getStatus().name());
        }
        pr.setStatus(PRStatus.APPROVED);
        pr = pullRequestRepository.save(pr);
        try {
            notificationService.sendFinalApprovalNotification(pr);
        } catch (Exception e) {
            log.warn("Failed to send final approval notifications: {}", e.getMessage());
        }
        return pr;
    }

    @Transactional
    public PullRequest reject(Long id, User currentUser) {
        PullRequest pr = pullRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pull request not found"));
        if (pr.getStatus() != PRStatus.IN_REVIEW) {
            throw new InvalidStateTransitionException(
                    "Cannot reject pull request. It must be in IN_REVIEW status, but it is currently "
                            + pr.getStatus().name());
        }
        pr.setStatus(PRStatus.REJECTED);
        pr = pullRequestRepository.save(pr);
        try {
            notificationService.sendRejectionNotification(pr, currentUser);
        } catch (Exception e) {
            log.warn("Failed to send rejection notification: {}", e.getMessage());
        }
        return pr;
    }

    @Transactional
    public PullRequest merge(Long id, User currentUser) {
        PullRequest pr = pullRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pull request not found"));
        if (pr.getStatus() != PRStatus.APPROVED) {
            throw new InvalidStateTransitionException(
                    "Cannot merge pull request. It must be in APPROVED status, but it is currently "
                            + pr.getStatus().name());
        }
        pr.setStatus(PRStatus.MERGED);
        pr = pullRequestRepository.save(pr);
        try {
            notificationService.sendMergedNotification(pr);
        } catch (Exception e) {
            log.warn("Failed to send merged notifications: {}", e.getMessage());
        }
        return pr;
    }

    public List<Review> getReviewsForPullRequest(PullRequest pr) {
        return reviewRepository.findByPullRequest(pr);
    }

    public List<User> getUsersWithReviewerRole() {
        return userRepository.findByRole(com.prreview.app.enums.Role.REVIEWER);
    }

    public long getTotalPRCount() {
        return pullRequestRepository.count();
    }

    public long getPRCountByStatus(PRStatus status) {
        return pullRequestRepository.countByStatus(status);
    }

    public List<PullRequest> findRecentPRsForUser(User user, int limit) {
        return pullRequestRepository.findByAuthorOrReviewersContaining(user, user).stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .limit(limit)
                .toList();
    }

    public List<PullRequest> findRecentPRsByAuthor(User author, int limit) {
        return pullRequestRepository.findByAuthor(author).stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .limit(limit)
                .toList();
    }

    public List<PullRequest> findPRsByStatus(PRStatus status) {
        return pullRequestRepository.findByStatus(status);
    }

    /**
     * Returns the most recently updated PRs across all users, ordered by updatedAt
     * descending.
     */
    public List<PullRequest> findRecentPRs(int limit) {
        return pullRequestRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "updatedAt"))).getContent();
    }
}
