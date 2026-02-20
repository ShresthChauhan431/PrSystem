package com.prreview.app.repository;

import com.prreview.app.model.Notification;
import com.prreview.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserAndIsReadFalse(User user);

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    long countByUserAndIsReadFalse(User user);
}
