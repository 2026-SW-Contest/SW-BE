package org.swbe.domain.lostitem.dto.response;

import java.util.List;

public record ItemClaimSliceResponse(
    List<ItemClaimListItemResponse> content,
    String nextCursor,
    boolean hasNext
) {

  public ItemClaimSliceResponse {
    content = List.copyOf(content);
  }
}
