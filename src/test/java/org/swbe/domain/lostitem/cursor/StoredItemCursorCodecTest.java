package org.swbe.domain.lostitem.cursor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.swbe.global.error.BusinessException;

class StoredItemCursorCodecTest {

  private final StoredItemCursorCodec codec = new StoredItemCursorCodec();

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

    StoredItemCursor result = codec.decode(codec.encode(createdAt, 25L));

    assertThat(result.createdAt()).isEqualTo(createdAt);
    assertThat(result.id()).isEqualTo(25L);
  }

  @Test
  void rejectsMalformedCursor() {
    assertThatThrownBy(() -> codec.decode("not-a-valid-cursor"))
        .isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode().code()
        ).isEqualTo("STORED_ITEM_INVALID_CURSOR"));
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
