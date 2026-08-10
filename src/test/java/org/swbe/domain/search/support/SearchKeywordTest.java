package org.swbe.domain.search.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.swbe.global.error.BusinessException;
import org.swbe.global.error.CommonErrorCode;

class SearchKeywordTest {

  @Test
  void keywordIsStrippedNormalizedAndEscaped() {
    SearchKeyword keyword = SearchKeyword.from("  AIR%_!  ");

    assertThat(keyword.value()).isEqualTo("AIR%_!");
    assertThat(keyword.normalized()).isEqualTo("air%_!");
    assertThat(keyword.containsPattern())
        .isEqualTo("%air!%!_!!%");
    assertThat(keyword.prefixPattern())
        .isEqualTo("air!%!_!!%");
  }

  @Test
  void invalidKeywordReturnsValidationError() {
    assertValidationError("   ");
    assertValidationError("a".repeat(101));
  }

  private void assertValidationError(String keyword) {
    assertThatThrownBy(() -> SearchKeyword.from(keyword))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(CommonErrorCode.VALIDATION_FAILED)
        );
  }
}
