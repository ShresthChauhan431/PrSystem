package com.prreview.app.service;

import com.prreview.app.enums.Role;
import com.prreview.app.model.Notification;
import com.prreview.app.model.PullRequest;
import com.prreview.app.model.Review;
import com.prreview.app.model.User;
import com.prreview.app.repository.NotificationRepository;
import com.prreview.app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class NotificationService {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Async
    @Transactional
    public void sendPRCreatedNotification(PullRequest pr) {
        List<User> recipients = new ArrayList<>();
        recipients.addAll(userRepository.findByRole(Role.TEAM_LEAD));
        recipients.addAll(userRepository.findByRole(Role.ADMIN));
        String subject = "New PR Submitted: " + pr.getTitle();
        String link = baseUrl + "/prs/" + pr.getId();
        String body = String.format(
                "A new pull request has been submitted.\n\nTitle: %s\nAuthor: %s\nBranch: %s\n\nView PR: %s",
                pr.getTitle(), pr.getAuthor().getName(), pr.getBranchName(), link);
        String inAppMessage = String.format("New PR: %s by %s. %s", pr.getTitle(), pr.getAuthor().getName(), link);
        for (User u : recipients) {
            sendEmailAndSaveNotification(u, subject, body, inAppMessage);
        }
    }

    @Async
    @Transactional
    public void sendReviewerAssignedNotification(PullRequest pr, List<User> assignedReviewers) {
        if (assignedReviewers == null || assignedReviewers.isEmpty())
            return;
        String subject = "You have been assigned to review PR #" + pr.getId();
        String link = baseUrl + "/prs/" + pr.getId();
        String body = String.format(
                "You have been assigned to review a pull request.\n\nTitle: %s\nAuthor: %s\n\nView PR: %s",
                pr.getTitle(), pr.getAuthor().getName(), link);
        String inAppMessage = String.format("You were assigned to review PR: %s. %s", pr.getTitle(), link);
        for (User u : assignedReviewers) {
            sendEmailAndSaveNotification(u, subject, body, inAppMessage);
        }
    }

    @Async
    @Transactional
    public void sendReviewSubmittedNotification(Review review) {
        PullRequest pr = review.getPullRequest();
        Set<User> recipients = new LinkedHashSet<>();
        recipients.add(pr.getAuthor());
        recipients.addAll(userRepository.findByRole(Role.TEAM_LEAD));
        String subject = String.format("Review submitted on PR #%d: %s", pr.getId(),
                review.getStatus().getDisplayName());
        String link = baseUrl + "/prs/" + pr.getId();
        String commentPart = (review.getComment() != null && !review.getComment().isEmpty())
                ? "\nComment: " + review.getComment() + "\n"
                : "\n";
        String body = String.format(
                "A review was submitted on pull request #%d.\n\nReviewer: %s\nStatus: %s%sView PR: %s",
                pr.getId(), review.getReviewer().getName(), review.getStatus().getDisplayName(), commentPart, link);
        String inAppMessage = String.format("Review by %s on PR #%d: %s. %s",
                review.getReviewer().getName(), pr.getId(), review.getStatus().getDisplayName(), link);
        for (User u : recipients) {
            sendEmailAndSaveNotification(u, subject, body, inAppMessage);
        }
    }

    @Async
    @Transactional
    public void sendFinalApprovalNotification(PullRequest pr) {
        Set<User> recipients = new LinkedHashSet<>();
        recipients.add(pr.getAuthor());
        if (pr.getReviewers() != null)
            recipients.addAll(pr.getReviewers());
        String subject = String.format("PR #%d has been Approved and is Deployment Ready", pr.getId());
        String link = baseUrl + "/prs/" + pr.getId();
        String body = String.format(
                "Pull request #%d has been approved and is ready for deployment.\n\nTitle: %s\n\nView PR: %s",
                pr.getId(), pr.getTitle(), link);
        String inAppMessage = String.format("PR #%d approved: %s. %s", pr.getId(), pr.getTitle(), link);
        for (User u : recipients) {
            sendEmailAndSaveNotification(u, subject, body, inAppMessage);
        }
    }

    @Async
    @Transactional
    public void sendRejectionNotification(PullRequest pr, User rejectedBy) {
        User author = pr.getAuthor();
        String subject = String.format("PR #%d has been Rejected", pr.getId());
        String link = baseUrl + "/prs/" + pr.getId();
        String body = String.format("Your pull request has been rejected.\n\nRejected by: %s\nTitle: %s\n\nView PR: %s",
                rejectedBy.getName(), pr.getTitle(), link);
        String inAppMessage = String.format("PR #%d rejected by %s: %s. %s", pr.getId(), rejectedBy.getName(),
                pr.getTitle(), link);
        sendEmailAndSaveNotification(author, subject, body, inAppMessage);
    }

    @Async
    @Transactional
    public void sendMergedNotification(PullRequest pr) {
        Set<User> recipients = new LinkedHashSet<>();
        recipients.add(pr.getAuthor());
        recipients.addAll(userRepository.findByRole(Role.TEAM_LEAD));
        String subject = String.format("PR #%d has been Merged", pr.getId());
        String link = baseUrl + "/prs/" + pr.getId();
        String body = String.format("Pull request #%d has been merged.\n\nTitle: %s\n\nView PR: %s", pr.getId(),
                pr.getTitle(), link);
        String inAppMessage = String.format("PR #%d merged: %s. %s", pr.getId(), pr.getTitle(), link);
        for (User u : recipients) {
            sendEmailAndSaveNotification(u, subject, body, inAppMessage);
        }
    }

    public long countUnreadForUser(User user) {
        if (user == null)
            return 0;
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    private void sendEmailAndSaveNotification(User user, String subject, String emailBody, String inAppMessage) {
        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(user.getEmail());
                message.setSubject(subject);
                message.setText(emailBody);
                mailSender.send(message);
            } catch (Exception e) {
                log.warn("Failed to send email to {}: {}", user.getEmail(), e.getMessage());
            }
        } else {
            log.debug("Mail sender not configured, skipping email to {}", user.getEmail());
        }
        Notification notification = Notification.builder()
                .user(user)
                .message(inAppMessage)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }
}
