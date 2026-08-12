package org.swbe.domain.notification.cursor;

import java.time.LocalDateTime;

public record NotificationCursor(
    LocalDateTime createdAt,
    Long id
) {
}
