package org.swbe.domain.notification.dto.response;

import java.time.LocalDateTime;

public record NotificationListItemResponse(
    Long notificationId,
    String notificationType,
    String title,
    String content,
    String referenceType,
    Long referenceId,
    boolean read,
    LocalDateTime readAt,
    LocalDateTime createdAt
) {
}
