package org.swbe.domain.facilityrequest.dto.response;

import java.util.List;

public record AdminFacilityRequestPageResponse(
    List<AdminFacilityRequestListItemResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {

  public AdminFacilityRequestPageResponse {
    content = List.copyOf(content);
  }
}
