package com.prreview.app.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/403";
    }

    @ExceptionHandler(IllegalStateException.class)
    public RedirectView handleIllegalState(IllegalStateException ex,
                                           HttpServletRequest request,
                                           RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        String referer = request.getHeader("Referer");
        String redirectUrl = (referer != null && !referer.isBlank()) ? referer : "/prs";
        return new RedirectView(redirectUrl);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public RedirectView handleInvalidStateTransition(InvalidStateTransitionException ex,
                                                     HttpServletRequest request,
                                                     RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        String referer = request.getHeader("Referer");
        String redirectUrl = (referer != null && !referer.isBlank()) ? referer : "/prs";
        return new RedirectView(redirectUrl);
    }
}
