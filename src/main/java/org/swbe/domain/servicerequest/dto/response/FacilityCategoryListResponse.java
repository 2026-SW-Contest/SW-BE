package org.swbe.domain.servicerequest.dto.response;

import java.util.List;

public record FacilityCategoryListResponse(
    List<FacilityCategoryResponse> data
) {

  public FacilityCategoryListResponse {
    data = List.copyOf(data);
  }
}
