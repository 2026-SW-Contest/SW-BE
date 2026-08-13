package org.swbe.domain.facilityrequest.dto.request;

import java.time.LocalDate;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;

public record FacilityRequestSearchCondition(
    Long categoryId,
    Long locationId,
    FacilityRequestStatus status,
    String keyword,
    LocalDate from,
    LocalDate to,
    String cursor,
    int size
) {

  public FacilityRequestSearchCondition {
    if (keyword != null) {
      keyword = keyword.trim();
      if (keyword.isEmpty()) {
        keyword = null;
      }
    }
  }
}
