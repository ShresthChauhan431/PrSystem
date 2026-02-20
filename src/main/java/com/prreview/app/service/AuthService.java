package com.prreview.app.service;

import com.prreview.app.dto.UserRegistrationDTO;
import com.prreview.app.model.User;
import com.prreview.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(UserRegistrationDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("An account already exists with this email address.");
        }
        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(encodedPassword)
                .role(dto.getRole())
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }
}
