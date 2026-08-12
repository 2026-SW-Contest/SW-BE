package org.swbe.domain.facilityrequest.dto.response;

import java.util.List;

public record MyFacilityRequestSliceResponse(
    List<FacilityRequestListItemResponse> content,
    String nextCursor,
    boolean hasNext
) {

  public MyFacilityRequestSliceResponse {
    content = List.copyOf(content);
  }
}
