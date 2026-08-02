package org.swbe.domain.campus.dto.response;

import java.util.List;

public record LocationListResponse(
    List<LocationResponse> data
) {

  public LocationListResponse {
    data = List.copyOf(data);
  }
}
