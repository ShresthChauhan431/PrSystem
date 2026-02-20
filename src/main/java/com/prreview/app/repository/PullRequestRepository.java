package com.prreview.app.repository;

import com.prreview.app.enums.PRStatus;
import com.prreview.app.model.PullRequest;
import com.prreview.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PullRequestRepository extends JpaRepository<PullRequest, Long> {

    List<PullRequest> findByAuthor(User author);

    List<PullRequest> findByStatus(PRStatus status);

    List<PullRequest> findByReviewersContaining(User reviewer);

    List<PullRequest> findByAuthorOrReviewersContaining(User author, User reviewer);

    long countByStatus(PRStatus status);
}
