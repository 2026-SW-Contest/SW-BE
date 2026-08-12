package org.swbe.domain.notification.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.swbe.domain.notification.entity.Notification;

public interface NotificationRepository
    extends JpaRepository<Notification, Long> {

  @Query("""
      SELECT notification
      FROM Notification notification
      WHERE notification.recipient.id = :recipientUserId
        AND (
          :cursorCreatedAt IS NULL
          OR notification.createdAt < :cursorCreatedAt
          OR (
            notification.createdAt = :cursorCreatedAt
            AND notification.id < :cursorId
          )
        )
      ORDER BY notification.createdAt DESC, notification.id DESC
      """)
  List<Notification> findAllByCursor(
      @Param("recipientUserId") Long recipientUserId,
      @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
      @Param("cursorId") Long cursorId,
      Pageable pageable
  );

  Optional<Notification> findByIdAndRecipient_Id(
      Long notificationId,
      Long recipientUserId
  );

  long countByRecipient_IdAndReadAtIsNull(Long recipientUserId);

  @Modifying(
      flushAutomatically = true,
      clearAutomatically = true
  )
  @Query("""
      UPDATE Notification notification
      SET notification.readAt = :readAt
      WHERE notification.recipient.id = :recipientUserId
        AND notification.readAt IS NULL
      """)
  int markAllAsRead(
      @Param("recipientUserId") Long recipientUserId,
      @Param("readAt") LocalDateTime readAt
  );
}
