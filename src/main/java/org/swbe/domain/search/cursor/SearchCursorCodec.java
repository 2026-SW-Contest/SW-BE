package org.swbe.domain.search.cursor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import org.springframework.stereotype.Component;
import org.swbe.domain.search.exception.SearchErrorCode;
import org.swbe.global.error.BusinessException;

@Component
public class SearchCursorCodec {

  private static final String DELIMITER = "\\|";

  public String encode(LocalDateTime createdAt, Long id) {
    String value = createdAt + "|" + id;

    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  public SearchCursor decode(String cursor) {
    try {
      String decoded = new String(
          Base64.getUrlDecoder().decode(cursor),
          StandardCharsets.UTF_8
      );
      String[] values = decoded.split(DELIMITER, 2);

      if (values.length != 2) {
        throw invalidCursor();
      }

      LocalDateTime createdAt = LocalDateTime.parse(values[0]);
      long id = Long.parseLong(values[1]);

      if (id <= 0) {
        throw invalidCursor();
      }

      return new SearchCursor(createdAt, id);
    } catch (
        IllegalArgumentException |
        DateTimeParseException exception
    ) {
      throw invalidCursor();
    }
  }

  private BusinessException invalidCursor() {
    return new BusinessException(SearchErrorCode.INVALID_CURSOR);
  }
}
