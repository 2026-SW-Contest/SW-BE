package org.swbe.domain.lostitem.dto.response;

import java.util.List;

public record StoredItemSliceResponse(
    List<StoredItemListItemResponse> content,
    String nextCursor,
    boolean hasNext
) {

  public StoredItemSliceResponse {
    content = List.copyOf(content);
  }
}
