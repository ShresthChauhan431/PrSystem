package com.prreview.app.repository;

import com.prreview.app.model.AuditLog;
import com.prreview.app.model.PullRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByPullRequestOrderByTimestampDesc(PullRequest pr);

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
