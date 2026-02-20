package com.prreview.app.config;

import com.prreview.app.enums.PRStatus;
import com.prreview.app.enums.ReviewStatus;
import com.prreview.app.enums.Role;
import com.prreview.app.model.PullRequest;
import com.prreview.app.model.Review;
import com.prreview.app.model.User;
import com.prreview.app.repository.PullRequestRepository;
import com.prreview.app.repository.ReviewRepository;
import com.prreview.app.repository.UserRepository;
import com.prreview.app.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
/*
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PullRequestRepository pullRequestRepository;
    private final ReviewRepository reviewRepository;
    private final AuditLogService auditLogService;

    public DataSeeder(UserRepository userRepository, 
                     PasswordEncoder passwordEncoder,
                     PullRequestRepository pullRequestRepository,
                     ReviewRepository reviewRepository,
                     AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.pullRequestRepository = pullRequestRepository;
        this.reviewRepository = reviewRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Seed users if they don't exist
        if (userRepository.count() == 0) {
            String encodedPassword = passwordEncoder.encode("password123");
            createUser("Alice Admin", "admin@test.com", encodedPassword, Role.ADMIN);
            createUser("Bob Lead", "lead@test.com", encodedPassword, Role.TEAM_LEAD);
            createUser("Carol Review", "reviewer@test.com", encodedPassword, Role.REVIEWER);
            createUser("Dave Dev", "dev@test.com", encodedPassword, Role.DEVELOPER);
            log.info("Seed users created (admin@test.com, lead@test.com, reviewer@test.com, dev@test.com / password123).");
        }

        // Seed pull requests if they don't exist
        if (pullRequestRepository.count() == 0) {
            seedPullRequests();
            log.info("Seed pull requests created successfully.");
        } else {
            log.debug("Pull requests already exist, skipping seed data.");
        }
    }

    private void createUser(String name, String email, String encodedPassword, Role role) {
        userRepository.save(new User(name, email, encodedPassword, role));
    }

    private void seedPullRequests() {
        User daveDev = userRepository.findByEmail("dev@test.com")
                .orElseThrow(() -> new IllegalStateException("Dave Dev user not found"));
        User bobLead = userRepository.findByEmail("lead@test.com")
                .orElseThrow(() -> new IllegalStateException("Bob Lead user not found"));
        User carolReview = userRepository.findByEmail("reviewer@test.com")
                .orElseThrow(() -> new IllegalStateException("Carol Review user not found"));

        LocalDateTime baseTime = LocalDateTime.now().minusDays(7);

        // 1. "Feature: User Authentication Overhaul" — author: Dave Dev, branch: feature/auth-v2, status: OPEN
        PullRequest pr1 = PullRequest.builder()
                .title("Feature: User Authentication Overhaul")
                .description("Complete overhaul of the authentication system with OAuth2 support and improved security.")
                .branchName("feature/auth-v2")
                .repoUrl("https://github.com/example/repo")
                .status(PRStatus.OPEN)
                .author(daveDev)
                .reviewers(new HashSet<>())
                .createdAt(baseTime)
                .updatedAt(baseTime)
                .build();
        pr1 = pullRequestRepository.save(pr1);
        auditLogService.logAction(pr1, AuditLogService.PR_CREATED, daveDev);

        // 2. "Fix: Database Connection Pool Leak" — author: Dave Dev, branch: fix/db-pool, status: IN_REVIEW, reviewer assigned: Carol Review
        PullRequest pr2 = PullRequest.builder()
                .title("Fix: Database Connection Pool Leak")
                .description("Fixed memory leak in database connection pool that was causing performance degradation.")
                .branchName("fix/db-pool")
                .repoUrl("https://github.com/example/repo")
                .status(PRStatus.IN_REVIEW)
                .author(daveDev)
                .reviewers(new HashSet<>(Set.of(carolReview)))
                .createdAt(baseTime.minusDays(5))
                .updatedAt(baseTime.minusDays(2))
                .build();
        pr2 = pullRequestRepository.save(pr2);
        auditLogService.logAction(pr2, AuditLogService.PR_CREATED, daveDev);
        auditLogService.logAction(pr2, AuditLogService.REVIEWER_ASSIGNED, daveDev);

        // 3. "Enhancement: Email Notification Templates" — author: Bob Lead, branch: enhancement/email-templates, status: IN_REVIEW, reviewer assigned: Carol Review
        // Also add a sample Review submitted by Carol Review with status APPROVED and comment "Looks good, minor formatting nits"
        PullRequest pr3 = PullRequest.builder()
                .title("Enhancement: Email Notification Templates")
                .description("Added customizable email templates for various notification types with HTML support.")
                .branchName("enhancement/email-templates")
                .repoUrl("https://github.com/example/repo")
                .status(PRStatus.IN_REVIEW)
                .author(bobLead)
                .reviewers(new HashSet<>(Set.of(carolReview)))
                .createdAt(baseTime.minusDays(4))
                .updatedAt(baseTime.minusDays(1))
                .build();
        pr3 = pullRequestRepository.save(pr3);
        auditLogService.logAction(pr3, AuditLogService.PR_CREATED, bobLead);
        auditLogService.logAction(pr3, AuditLogService.REVIEWER_ASSIGNED, bobLead);
        
        Review review3 = Review.builder()
                .pullRequest(pr3)
                .reviewer(carolReview)
                .comment("Looks good, minor formatting nits")
                .status(ReviewStatus.APPROVED)
                .reviewedAt(baseTime.minusDays(1))
                .build();
        reviewRepository.save(review3);
        auditLogService.logAction(pr3, AuditLogService.REVIEW_SUBMITTED, carolReview);

        // 4. "Refactor: Service Layer Clean-up" — author: Dave Dev, branch: refactor/service-layer, status: APPROVED, reviewer: Carol Review, review status: APPROVED
        PullRequest pr4 = PullRequest.builder()
                .title("Refactor: Service Layer Clean-up")
                .description("Refactored service layer to improve separation of concerns and reduce coupling.")
                .branchName("refactor/service-layer")
                .repoUrl("https://github.com/example/repo")
                .status(PRStatus.APPROVED)
                .author(daveDev)
                .reviewers(new HashSet<>(Set.of(carolReview)))
                .createdAt(baseTime.minusDays(6))
                .updatedAt(baseTime.minusDays(3))
                .build();
        pr4 = pullRequestRepository.save(pr4);
        auditLogService.logAction(pr4, AuditLogService.PR_CREATED, daveDev);
        auditLogService.logAction(pr4, AuditLogService.REVIEWER_ASSIGNED, daveDev);
        
        Review review4 = Review.builder()
                .pullRequest(pr4)
                .reviewer(carolReview)
                .comment("Excellent refactoring work. Code is much cleaner now.")
                .status(ReviewStatus.APPROVED)
                .reviewedAt(baseTime.minusDays(3))
                .build();
        reviewRepository.save(review4);
        auditLogService.logAction(pr4, AuditLogService.REVIEW_SUBMITTED, carolReview);
        auditLogService.logAction(pr4, AuditLogService.PR_APPROVED, carolReview);

        // 5. "Feature: Advanced Search Filters" — author: Dave Dev, branch: feature/search, status: REJECTED, review: Carol Review, review status: REJECTED, comment: "Performance concerns with full-table scans"
        PullRequest pr5 = PullRequest.builder()
                .title("Feature: Advanced Search Filters")
                .description("Added advanced search filters with multiple criteria support.")
                .branchName("feature/search")
                .repoUrl("https://github.com/example/repo")
                .status(PRStatus.REJECTED)
                .author(daveDev)
                .reviewers(new HashSet<>(Set.of(carolReview)))
                .createdAt(baseTime.minusDays(8))
                .updatedAt(baseTime.minusDays(4))
                .build();
        pr5 = pullRequestRepository.save(pr5);
        auditLogService.logAction(pr5, AuditLogService.PR_CREATED, daveDev);
        auditLogService.logAction(pr5, AuditLogService.REVIEWER_ASSIGNED, daveDev);
        
        Review review5 = Review.builder()
                .pullRequest(pr5)
                .reviewer(carolReview)
                .comment("Performance concerns with full-table scans")
                .status(ReviewStatus.REJECTED)
                .reviewedAt(baseTime.minusDays(4))
                .build();
        reviewRepository.save(review5);
        auditLogService.logAction(pr5, AuditLogService.REVIEW_SUBMITTED, carolReview);
        auditLogService.logAction(pr5, AuditLogService.PR_REJECTED, carolReview);

        // 6. "Security Patch: CSRF Hardening" — author: Bob Lead, branch: security/csrf-patch, status: MERGED
        PullRequest pr6 = PullRequest.builder()
                .title("Security Patch: CSRF Hardening")
                .description("Enhanced CSRF protection with token validation and SameSite cookie attributes.")
                .branchName("security/csrf-patch")
                .repoUrl("https://github.com/example/repo")
                .status(PRStatus.MERGED)
                .author(bobLead)
                .reviewers(new HashSet<>(Set.of(carolReview)))
                .createdAt(baseTime.minusDays(10))
                .updatedAt(baseTime.minusDays(6))
                .build();
        pr6 = pullRequestRepository.save(pr6);
        auditLogService.logAction(pr6, AuditLogService.PR_CREATED, bobLead);
        auditLogService.logAction(pr6, AuditLogService.REVIEWER_ASSIGNED, bobLead);
        
        Review review6 = Review.builder()
                .pullRequest(pr6)
                .reviewer(carolReview)
                .comment("Security improvements look solid. Ready to merge.")
                .status(ReviewStatus.APPROVED)
                .reviewedAt(baseTime.minusDays(7))
                .build();
        reviewRepository.save(review6);
        auditLogService.logAction(pr6, AuditLogService.REVIEW_SUBMITTED, carolReview);
        auditLogService.logAction(pr6, AuditLogService.PR_APPROVED, carolReview);
        auditLogService.logAction(pr6, AuditLogService.PR_MERGED, bobLead);

        log.info("Seeded 6 pull requests with reviews and audit logs.");
    }
}
*/
public class DataSeeder {

}