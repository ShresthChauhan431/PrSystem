package com.prreview.app.controller;

import com.prreview.app.dto.UserRegistrationDTO;
import com.prreview.app.enums.Role;
import com.prreview.app.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("userRegistrationDTO", new UserRegistrationDTO());
        model.addAttribute("roles", Role.values());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("userRegistrationDTO") UserRegistrationDTO dto,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "register";
        }
        try {
            authService.registerUser(dto);
            redirectAttributes.addFlashAttribute("message", "Registration successful. Please log in.");
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "email.exists", e.getMessage());
            model.addAttribute("roles", Role.values());
            return "register";
        }
    }
}
