package org.swbe.domain.search.support;

import java.util.Locale;
import org.swbe.global.error.BusinessException;
import org.swbe.global.error.CommonErrorCode;

public record SearchKeyword(
    String value,
    String normalized,
    String containsPattern,
    String prefixPattern
) {

  private static final int MAX_LENGTH = 100;

  public static SearchKeyword from(String input) {
    if (input == null) {
      throw validationError();
    }

    String value = input.strip();
    if (value.isEmpty() || value.length() > MAX_LENGTH) {
      throw validationError();
    }

    String normalized = value.toLowerCase(Locale.ROOT);
    String escaped = escape(normalized);
    return new SearchKeyword(
        value,
        normalized,
        "%" + escaped + "%",
        escaped + "%"
    );
  }

  private static String escape(String value) {
    return value
        .replace("!", "!!")
        .replace("%", "!%")
        .replace("_", "!_");
  }

  private static BusinessException validationError() {
    return new BusinessException(
        CommonErrorCode.VALIDATION_FAILED
    );
  }
}
