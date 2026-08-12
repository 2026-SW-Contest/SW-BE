package org.swbe.domain.lostitem.dto.response;

import java.util.List;

public record MyItemClaimSliceResponse(
    List<MyItemClaimListItemResponse> content,
    String nextCursor,
    boolean hasNext
) {

  public MyItemClaimSliceResponse {
    content = List.copyOf(content);
  }
}
