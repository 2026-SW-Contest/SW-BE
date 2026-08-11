package org.swbe.domain.lostitem.dto.response;

import java.util.List;

public record ItemCategoryListResponse(
    List<ItemCategoryResponse> data
) {

  public ItemCategoryListResponse {
    data = List.copyOf(data);
  }
}
