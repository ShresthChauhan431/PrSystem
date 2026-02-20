package com.prreview.app.controller;

import com.prreview.app.enums.PRStatus;
import com.prreview.app.enums.Role;
import com.prreview.app.model.PullRequest;
import com.prreview.app.model.User;
import com.prreview.app.repository.UserRepository;
import com.prreview.app.service.PullRequestService;
import com.prreview.app.service.ReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping
public class DashboardController {

    private final PullRequestService pullRequestService;
    private final ReviewService reviewService;
    private final UserRepository userRepository;

    public DashboardController(PullRequestService pullRequestService,
                               ReviewService reviewService,
                               UserRepository userRepository) {
        this.pullRequestService = pullRequestService;
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User currentUser, Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("currentUser", currentUser);
        Role role = currentUser.getRole();

        if (role == Role.ADMIN || role == Role.TEAM_LEAD) {
            model.addAttribute("totalPRs", pullRequestService.getTotalPRCount());
            model.addAttribute("openPRs", pullRequestService.getPRCountByStatus(PRStatus.OPEN));
            model.addAttribute("inReviewPRs", pullRequestService.getPRCountByStatus(PRStatus.IN_REVIEW));
            model.addAttribute("approvedPRs", pullRequestService.getPRCountByStatus(PRStatus.APPROVED));
            model.addAttribute("rejectedPRs", pullRequestService.getPRCountByStatus(PRStatus.REJECTED));
            model.addAttribute("mergedPRs", pullRequestService.getPRCountByStatus(PRStatus.MERGED));
            model.addAttribute("totalUsers", userRepository.count());
            model.addAttribute("pendingApprovals", pullRequestService.findPRsByStatus(PRStatus.IN_REVIEW));
            model.addAttribute("deploymentReadyPRs", pullRequestService.findPRsByStatus(PRStatus.APPROVED));
        }
        if (role == Role.ADMIN) {
            model.addAttribute("recentPRs", pullRequestService.findRecentPRs(10));
        }

        if (role == Role.REVIEWER) {
            model.addAttribute("pendingReviews", reviewService.getPendingReviewsForUser(currentUser));
            model.addAttribute("completedReviews", reviewService.findReviewsByReviewer(currentUser).size());
            model.addAttribute("recentActivity", pullRequestService.findRecentPRsForUser(currentUser, 10));
        }

        if (role == Role.DEVELOPER) {
            List<PullRequest> myPRs = pullRequestService.findPullRequestsForUser(currentUser).stream()
                    .filter(pr -> pr.getAuthor().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
            model.addAttribute("myPRs", myPRs);
            model.addAttribute("openPRsOwn", myPRs.stream().filter(pr -> pr.getStatus() == PRStatus.OPEN).count());
            model.addAttribute("approvedPRsOwn", myPRs.stream().filter(pr -> pr.getStatus() == PRStatus.APPROVED).count());
            model.addAttribute("rejectedPRsOwn", myPRs.stream().filter(pr -> pr.getStatus() == PRStatus.REJECTED).count());
            model.addAttribute("recentMyPRs", pullRequestService.findRecentPRsByAuthor(currentUser, 5));
        }

        return "dashboard";
    }
}
