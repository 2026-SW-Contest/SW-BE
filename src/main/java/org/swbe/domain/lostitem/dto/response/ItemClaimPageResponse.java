package org.swbe.domain.lostitem.dto.response;

import java.util.List;

public record ItemClaimPageResponse(
    List<ItemClaimListItemResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {

  public ItemClaimPageResponse {
    content = List.copyOf(content);
  }
}
