package org.swbe.domain.servicerequest.dto.request;

import java.time.LocalDate;
import org.swbe.domain.servicerequest.entity.ServiceRequestStatus;

public record ServiceRequestSearchCondition(
    Long categoryId,
    Long locationId,
    ServiceRequestStatus status,
    String keyword,
    LocalDate from,
    LocalDate to,
    int page,
    int size
) {

  public ServiceRequestSearchCondition {
    if (keyword != null) {
      keyword = keyword.trim();
      if (keyword.isEmpty()) {
        keyword = null;
      }
    }
  }
}
