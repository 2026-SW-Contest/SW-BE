package org.swbe.domain.lostitem.dto.response;

import java.util.List;

public record LostItemOfficeListResponse(
    List<LostItemOfficeResponse> data
) {

  public LostItemOfficeListResponse {
    data = List.copyOf(data);
  }
}
