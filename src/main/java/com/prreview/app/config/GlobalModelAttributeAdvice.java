package com.prreview.app.config;

import com.prreview.app.model.User;
import com.prreview.app.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributeAdvice {

    private final NotificationService notificationService;

    public GlobalModelAttributeAdvice(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ModelAttribute("unreadNotificationCount")
    public long addUnreadNotificationCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null || !(auth.getPrincipal() instanceof User)) {
            return 0;
        }
        return notificationService.countUnreadForUser((User) auth.getPrincipal());
    }
}
