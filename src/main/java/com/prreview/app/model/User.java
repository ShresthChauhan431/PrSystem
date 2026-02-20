package com.prreview.app.model;

import com.prreview.app.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

/**
 * User entity implementing Spring Security's {@link org.springframework.security.core.userdetails.UserDetails}.
 * getAuthorities() returns a single SimpleGrantedAuthority with "ROLE_" + role name.
 * getUsername() returns email; getPassword() returns password.
 * isAccountNonExpired(), isAccountNonLocked(), isCredentialsNonExpired() return true; isEnabled() uses the enabled field.
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = true)
    private String expertiseTags;

    @Builder.Default
    @Column(nullable = false)
    private Integer weeklyReviewCount = 0;

    @Column(nullable = true)
    private LocalDate lastReviewResetDate;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Constructor for seed data / programmatic creation without Lombok builder. */
    public User(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.enabled = true;
        this.weeklyReviewCount = 0;
        this.lastReviewResetDate = null;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Returns true if this reviewer is overloaded given a weekly review limit.
     * Resets the weekly counter when a new week starts or when it has never been set.
     */
    public boolean isOverloaded(int weeklyLimit) {
        LocalDate today = LocalDate.now();
        java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.ISO;

        boolean needsReset =
                lastReviewResetDate == null
                        || today.getYear() != lastReviewResetDate.getYear()
                        || today.get(weekFields.weekOfWeekBasedYear()) !=
                           lastReviewResetDate.get(weekFields.weekOfWeekBasedYear());

        if (needsReset) {
            weeklyReviewCount = 0;
            lastReviewResetDate = today;
        }

        return weeklyReviewCount >= weeklyLimit;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
