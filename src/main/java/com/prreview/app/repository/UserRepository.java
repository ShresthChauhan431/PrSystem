package com.prreview.app.repository;

import com.prreview.app.enums.Role;
import com.prreview.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);

    boolean existsByEmail(String email);
}
