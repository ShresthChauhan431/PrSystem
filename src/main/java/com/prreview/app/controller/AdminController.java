package com.prreview.app.controller;

import com.prreview.app.enums.Role;
import com.prreview.app.model.User;
import com.prreview.app.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public String adminPanel(Model model) {
        List<User> users = userRepository.findAll();
        Map<Role, Long> countByRole = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            countByRole.put(role, users.stream().filter(u -> u.getRole() == role).count());
        }
        model.addAttribute("users", users);
        model.addAttribute("countByRole", countByRole);
        model.addAttribute("totalUsers", users.size());
        return "admin-panel";
    }
}
