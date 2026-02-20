package com.prreview.app.repository;

import com.prreview.app.model.PullRequest;
import com.prreview.app.model.Review;
import com.prreview.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByPullRequest(PullRequest pr);

    List<Review> findByReviewer(User reviewer);

    Optional<Review> findByPullRequestAndReviewer(PullRequest pr, User reviewer);
}
