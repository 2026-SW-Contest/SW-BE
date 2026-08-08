package org.swbe.domain.search.cursor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.swbe.domain.search.exception.SearchErrorCode;
import org.swbe.global.error.BusinessException;

class SearchCursorCodecTest {

  private final SearchCursorCodec codec = new SearchCursorCodec();

  @Test
  void encodedCursorCanBeDecoded() {
    LocalDateTime createdAt =
        LocalDateTime.of(2026, 8, 8, 12, 30);

    String encoded = codec.encode(createdAt, 25L);
    SearchCursor decoded = codec.decode(encoded);

    assertThat(decoded.createdAt()).isEqualTo(createdAt);
    assertThat(decoded.id()).isEqualTo(25L);
  }

  @Test
  void malformedCursorReturnsBusinessError() {
    assertInvalidCursor("not-a-valid-cursor");
    assertInvalidCursor(encode("invalid-date|25"));
    assertInvalidCursor(encode("2026-08-08T12:30:00|0"));
  }

  private void assertInvalidCursor(String cursor) {
    assertThatThrownBy(() -> codec.decode(cursor))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(SearchErrorCode.INVALID_CURSOR)
        );
  }

  private String encode(String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }
}
