package org.swbe.domain.facilityrequest.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FacilityRequestUpdateRequest(
    @Positive(message = "categoryId must be positive")
    Long categoryId,

    @Positive(message = "locationId must be positive")
    Long locationId,

    @Size(
        min = 1,
        max = 200,
        message = "title must be between 1 and 200 characters"
    )
    String title,

    @Size(
        min = 1,
        max = 500,
        message = "description must be between 1 and 500 characters"
    )
    String description,

    List<@Positive(message = "fileId must be positive") Long>
    keepFileIds
) {

  // 앞뒤 공백을 제거해 공백만 전달된 제목과 내용을 검증할 수 있게 한다.
  public FacilityRequestUpdateRequest {
    title = stripNullable(title);
    description = stripNullable(description);
  }

  // 문의 정보나 기존 첨부파일 목록 중 변경 요청이 존재하는지 확인한다.
  public boolean hasChanges() {
    return categoryId != null
        || locationId != null
        || title != null
        || description != null
        || keepFileIds != null;
  }

  // 값이 전달된 경우에만 문자열의 앞뒤 공백을 제거한다.
  private static String stripNullable(String value) {
    return value == null ? null : value.strip();
  }
}
