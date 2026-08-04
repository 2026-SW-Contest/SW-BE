package org.swbe.domain.facilityrequest.dto.response;

import java.util.List;

public record FacilityRequestPageResponse(
    List<FacilityRequestListItemResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {

  public FacilityRequestPageResponse {
    content = List.copyOf(content);
  }
}
