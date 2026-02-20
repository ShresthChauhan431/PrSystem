package com.prreview.app.service;

import com.prreview.app.dto.ReviewDTO;
import com.prreview.app.exception.ResourceNotFoundException;
import com.prreview.app.model.PullRequest;
import com.prreview.app.model.Review;
import com.prreview.app.model.User;
import com.prreview.app.repository.PullRequestRepository;
import com.prreview.app.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReviewService {

    private final PullRequestRepository pullRequestRepository;
    private final ReviewRepository reviewRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public Review submitReview(Long prId, ReviewDTO dto, User reviewer) {
        PullRequest pr = pullRequestRepository.findById(prId)
                .orElseThrow(() -> new ResourceNotFoundException("Pull request not found: " + prId));

        if (!pr.getReviewers().contains(reviewer)) {
            throw new IllegalStateException("You are not assigned as a reviewer for this pull request.");
        }

        Review review = reviewRepository.findByPullRequestAndReviewer(pr, reviewer)
                .orElse(null);

        if (review != null) {
            review.setComment(dto.getComment());
            review.setStatus(dto.getStatus());
            review.setReviewedAt(LocalDateTime.now());
            review = reviewRepository.save(review);
        } else {
            review = Review.builder()
                    .pullRequest(pr)
                    .reviewer(reviewer)
                    .comment(dto.getComment())
                    .status(dto.getStatus())
                    .reviewedAt(LocalDateTime.now())
                    .build();
            review = reviewRepository.save(review);
        }

        auditLogService.logAction(pr, AuditLogService.REVIEW_SUBMITTED, reviewer);
        try {
            notificationService.sendReviewSubmittedNotification(review);
        } catch (Exception e) {
            log.warn("Failed to send review submitted notifications: {}", e.getMessage());
        }
        return review;
    }

    public List<Review> findReviewsByPR(Long prId) {
        PullRequest pr = pullRequestRepository.findById(prId)
                .orElseThrow(() -> new ResourceNotFoundException("Pull request not found: " + prId));
        return reviewRepository.findByPullRequest(pr);
    }

    public List<Review> findReviewsByReviewer(User reviewer) {
        return reviewRepository.findByReviewer(reviewer);
    }

    public List<PullRequest> getPendingReviewsForUser(User reviewer) {
        List<PullRequest> assigned = pullRequestRepository.findByReviewersContaining(reviewer);
        return assigned.stream()
                .filter(pr -> reviewRepository.findByPullRequestAndReviewer(pr, reviewer).isEmpty())
                .toList();
    }
}
