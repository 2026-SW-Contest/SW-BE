package org.swbe.domain.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.swbe.domain.user.entity.AppUser;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "notification_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recipient_user_id")
  private AppUser recipient;

  @Column(name = "notification_type", nullable = false, length = 50)
  private String notificationType;

  @Column(name = "reference_type", length = 50)
  private String referenceType;

  @Column(name = "reference_id")
  private Long referenceId;

  @Column(nullable = false, length = 200)
  private String title;

  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(nullable = false)
  private String content;

  @Column(name = "delivery_channel", nullable = false, length = 30)
  private String deliveryChannel = "WEB";

  @Column(name = "delivery_status", nullable = false, length = 30)
  private String deliveryStatus = "PENDING";

  @Column(name = "event_key", unique = true, length = 150)
  private String eventKey;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  @Column(name = "sent_at")
  private LocalDateTime sentAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  private Notification(
      AppUser recipient,
      String notificationType,
      String referenceType,
      Long referenceId,
      String title,
      String content,
      LocalDateTime createdAt
  ) {
    this.recipient = Objects.requireNonNull(recipient);
    this.notificationType = requireText(
        notificationType,
        "notificationType"
    );
    this.referenceType = requireText(referenceType, "referenceType");
    this.referenceId = Objects.requireNonNull(referenceId);
    this.title = requireText(title, "title");
    this.content = requireText(content, "content");
    this.deliveryChannel = "WEB";
    this.deliveryStatus = "PENDING";
    this.createdAt = Objects.requireNonNull(createdAt);
  }

  // 시설문의 처리 결과를 작성자에게 전달할 웹 알림을 생성한다.
  public static Notification createFacilityRequestUpdate(
      AppUser recipient,
      Long facilityRequestId,
      String title,
      String content,
      LocalDateTime createdAt
  ) {
    return new Notification(
        recipient,
        "FACILITY_REQUEST_UPDATED",
        "FACILITY_REQUEST",
        facilityRequestId,
        title,
        content,
        createdAt
    );
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.strip();
  }
}
