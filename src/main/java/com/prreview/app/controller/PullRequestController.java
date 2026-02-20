package com.prreview.app.controller;

import com.prreview.app.dto.PullRequestDTO;
import com.prreview.app.dto.ReviewDTO;
import com.prreview.app.enums.PRStatus;
import com.prreview.app.model.PullRequest;
import com.prreview.app.model.User;
import com.prreview.app.service.AuditLogService;
import com.prreview.app.service.PullRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/prs")
@RequiredArgsConstructor
public class PullRequestController {

    private final PullRequestService pullRequestService;
    private final AuditLogService auditLogService;

    @GetMapping
    public String list(@AuthenticationPrincipal User currentUser,
                      @RequestParam(required = false) String status,
                      Model model) {
        List<PullRequest> pullRequests = pullRequestService.findPullRequestsForUser(currentUser);
        if (status != null && !status.isBlank()) {
            pullRequests = pullRequests.stream()
                    .filter(pr -> pr.getStatus().name().equals(status))
                    .toList();
        }
        model.addAttribute("pullRequests", pullRequests);
        model.addAttribute("statuses", PRStatus.values());
        model.addAttribute("selectedStatus", status);
        return "pr-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pullRequestDTO", new PullRequestDTO());
        return "pr-create";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("pullRequestDTO") PullRequestDTO dto,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal User currentUser,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "pr-create";
        }
        PullRequest pr = pullRequestService.createPullRequest(dto, currentUser);
        auditLogService.logAction(pr, AuditLogService.PR_CREATED, currentUser);
        redirectAttributes.addFlashAttribute("success", "Pull request created successfully.");
        return "redirect:/prs/" + pr.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal User currentUser,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        return pullRequestService.findById(id)
                .map(pr -> {
                    model.addAttribute("pullRequest", pr);
                    model.addAttribute("reviews", pullRequestService.getReviewsForPullRequest(pr));
                    model.addAttribute("reviewers", pullRequestService.getUsersWithReviewerRole());
                    model.addAttribute("auditLogs", auditLogService.findByPullRequest(pr));
                    model.addAttribute("currentUser", currentUser);
                    model.addAttribute("reviewStatuses", com.prreview.app.enums.ReviewStatus.values());
                    ReviewDTO reviewDTO = new ReviewDTO();
                    reviewDTO.setPrId(pr.getId());
                    model.addAttribute("reviewDTO", reviewDTO);
                    return "pr-detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Pull request not found.");
                    return "redirect:/prs";
                });
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal User currentUser,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        return pullRequestService.findById(id)
                .map(pr -> {
                    if (pr.getStatus() != PRStatus.OPEN) {
                        redirectAttributes.addFlashAttribute("error", "Only OPEN pull requests can be edited.");
                        return "redirect:/prs/" + id;
                    }
                    model.addAttribute("pullRequest", pr);
                    model.addAttribute("pullRequestDTO", toDTO(pr));
                    return "pr-edit";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Pull request not found.");
                    return "redirect:/prs";
                });
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("pullRequestDTO") PullRequestDTO dto,
                       BindingResult bindingResult,
                       @AuthenticationPrincipal User currentUser,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            pullRequestService.findById(id).ifPresent(pr -> model.addAttribute("pullRequest", pr));
            return "pr-edit";
        }
        try {
            PullRequest pr = pullRequestService.updatePullRequest(id, dto, currentUser);
            auditLogService.logAction(pr, AuditLogService.PR_UPDATED, currentUser);
            redirectAttributes.addFlashAttribute("success", "Pull request updated successfully.");
            return "redirect:/prs/" + pr.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/prs/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable Long id,
                         @RequestParam(value = "reviewerIds", required = false) List<Long> reviewerIds,
                         @AuthenticationPrincipal User currentUser,
                         RedirectAttributes redirectAttributes) {
        try {
            PullRequest pr = pullRequestService.assignReviewers(id, reviewerIds != null ? reviewerIds : List.of(), currentUser);
            auditLogService.logAction(pr, AuditLogService.REVIEWER_ASSIGNED, currentUser);
            redirectAttributes.addFlashAttribute("success", "Reviewers assigned and status set to In Review.");
            return "redirect:/prs/" + pr.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/prs/" + id;
        }
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                         @AuthenticationPrincipal User currentUser,
                         RedirectAttributes redirectAttributes) {
        try {
            PullRequest pr = pullRequestService.approve(id, currentUser);
            auditLogService.logAction(pr, AuditLogService.PR_APPROVED, currentUser);
            redirectAttributes.addFlashAttribute("success", "Pull request approved.");
            return "redirect:/prs/" + pr.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/prs/" + id;
        }
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @AuthenticationPrincipal User currentUser,
                         RedirectAttributes redirectAttributes) {
        try {
            PullRequest pr = pullRequestService.reject(id, currentUser);
            auditLogService.logAction(pr, AuditLogService.PR_REJECTED, currentUser);
            redirectAttributes.addFlashAttribute("error", "Pull request rejected.");
            return "redirect:/prs/" + pr.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/prs/" + id;
        }
    }

    @PostMapping("/{id}/merge")
    public String merge(@PathVariable Long id,
                        @AuthenticationPrincipal User currentUser,
                        RedirectAttributes redirectAttributes) {
        try {
            PullRequest pr = pullRequestService.merge(id, currentUser);
            auditLogService.logAction(pr, AuditLogService.PR_MERGED, currentUser);
            redirectAttributes.addFlashAttribute("success", "Pull request merged.");
            return "redirect:/prs/" + pr.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/prs/" + id;
        }
    }

    private static PullRequestDTO toDTO(PullRequest pr) {
        return PullRequestDTO.builder()
                .title(pr.getTitle())
                .description(pr.getDescription())
                .branchName(pr.getBranchName())
                .repoUrl(pr.getRepoUrl())
                .build();
    }
}
