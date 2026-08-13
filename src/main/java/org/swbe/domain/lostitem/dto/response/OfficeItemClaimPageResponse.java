package org.swbe.domain.lostitem.dto.response;

import java.util.List;

public record OfficeItemClaimPageResponse(
    List<OfficeItemClaimListItemResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {

  public OfficeItemClaimPageResponse {
    content = List.copyOf(content);
  }
}
