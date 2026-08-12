package org.swbe.domain.facilityrequest.dto.request;

import java.time.LocalDate;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;

public record AdminFacilityRequestSearchCondition(
    String keyword,
    FacilityRequestStatus status,
    Long categoryId,
    Long locationId,
    LocalDate from,
    LocalDate to,
    int page,
    int size
) {

  public AdminFacilityRequestSearchCondition {
    if (keyword != null) {
      keyword = keyword.trim();
      if (keyword.isEmpty()) {
        keyword = null;
      }
    }
  }
}
