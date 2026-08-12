package org.swbe.domain.notification.cursor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.swbe.global.error.BusinessException;

class NotificationCursorCodecTest {

  private final NotificationCursorCodec codec =
      new NotificationCursorCodec();

  @Test
  void encodesAndDecodesCreatedAtAndId() {
    LocalDateTime createdAt = LocalDateTime.of(
        2026,
        8,
        12,
        14,
        30,
        15,
        123_000_000
    );

    NotificationCursor result = codec.decode(
        codec.encode(createdAt, 10L)
    );

    assertThat(result.createdAt()).isEqualTo(createdAt);
    assertThat(result.id()).isEqualTo(10L);
  }

  @Test
  void rejectsMalformedCursor() {
    assertThatThrownBy(() -> codec.decode("not-a-valid-cursor"))
        .isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode().code()
        ).isEqualTo("NOTIFICATION_INVALID_CURSOR"));
  }

  @Test
  void rejectsNonPositiveId() {
    String cursor = codec.encode(
        LocalDateTime.of(2026, 8, 12, 14, 30),
        0L
    );

    assertThatThrownBy(() -> codec.decode(cursor))
        .isInstanceOf(BusinessException.class);
  }
}
