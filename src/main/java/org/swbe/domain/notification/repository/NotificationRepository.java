package org.swbe.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.notification.entity.Notification;

public interface NotificationRepository
    extends JpaRepository<Notification, Long> {
}
