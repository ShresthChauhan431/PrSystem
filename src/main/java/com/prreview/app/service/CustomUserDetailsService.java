package com.prreview.app.service;

import com.prreview.app.model.User;
import com.prreview.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Username is the email address
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        // User entity implements UserDetails: getAuthorities() maps Role to ROLE_* GrantedAuthority,
        // getUsername() returns email, getPassword() returns password, isEnabled() uses enabled field
        return user;
    }
}
