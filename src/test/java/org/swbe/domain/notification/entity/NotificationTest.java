package org.swbe.domain.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.swbe.domain.user.entity.AppUser;

class NotificationTest {

  @Test
  void firstReadTimeIsPreservedWhenMarkedAsReadAgain() {
    Notification notification = Notification.createItemClaimDecision(
        mock(AppUser.class),
        25L,
        "소유자 확인 요청이 승인되었습니다.",
        "학생증과 물품 특징을 확인했습니다.",
        LocalDateTime.of(2026, 8, 12, 14, 30)
    );
    LocalDateTime firstReadAt = LocalDateTime.of(
        2026, 8, 12, 15, 0
    );

    notification.markAsRead(firstReadAt);
    notification.markAsRead(firstReadAt.plusMinutes(10));

    assertThat(notification.getReadAt()).isEqualTo(firstReadAt);
  }
}
