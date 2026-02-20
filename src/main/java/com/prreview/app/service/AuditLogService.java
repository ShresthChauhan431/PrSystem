package com.prreview.app.service;

import com.prreview.app.model.AuditLog;
import com.prreview.app.model.PullRequest;
import com.prreview.app.model.User;
import com.prreview.app.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    public static final String PR_CREATED = "PR Created";
    public static final String PR_UPDATED = "PR Updated";
    public static final String REVIEWER_ASSIGNED = "Reviewer Assigned";
    public static final String REVIEW_SUBMITTED = "Review Submitted";
    public static final String PR_APPROVED = "PR Approved";
    public static final String PR_REJECTED = "PR Rejected";
    public static final String PR_MERGED = "PR Merged";

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog logAction(PullRequest pr, String action, User performedBy) {
        AuditLog log = AuditLog.builder()
                .pullRequest(pr)
                .action(action)
                .performedBy(performedBy)
                .timestamp(LocalDateTime.now())
                .build();
        return auditLogRepository.save(log);
    }

    public List<AuditLog> findByPullRequest(PullRequest pr) {
        return auditLogRepository.findByPullRequestOrderByTimestampDesc(pr);
    }

    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }
}
