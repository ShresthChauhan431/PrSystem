package com.prreview.app.controller;

import com.prreview.app.dto.ReviewDTO;
import com.prreview.app.model.User;
import com.prreview.app.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public String submitReview(@Valid ReviewDTO dto,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal User currentUser,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Validation failed. Please check your input.");
            return "redirect:/prs/" + (dto.getPrId() != null ? dto.getPrId() : "");
        }

        try {
            reviewService.submitReview(dto.getPrId(), dto, currentUser);
            redirectAttributes.addFlashAttribute("success", "Review submitted successfully");
            return "redirect:/prs/" + dto.getPrId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/prs/" + dto.getPrId();
        }
    }
}
