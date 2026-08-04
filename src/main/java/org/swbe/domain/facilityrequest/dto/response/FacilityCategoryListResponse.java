package org.swbe.domain.facilityrequest.dto.response;

import java.util.List;

public record FacilityCategoryListResponse(
    List<FacilityCategoryResponse> data
) {

  public FacilityCategoryListResponse {
    data = List.copyOf(data);
  }
}
