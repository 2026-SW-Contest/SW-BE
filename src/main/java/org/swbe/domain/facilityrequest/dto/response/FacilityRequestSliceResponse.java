package org.swbe.domain.facilityrequest.dto.response;

import java.util.List;

public record FacilityRequestSliceResponse(
    List<FacilityRequestListItemResponse> content,
    String nextCursor,
    boolean hasNext
) {

  public FacilityRequestSliceResponse {
    content = List.copyOf(content);
  }
}
