package org.swbe.domain.lostitem.dto.response;

import java.util.List;

public record OfficeItemClaimSliceResponse(
    List<OfficeItemClaimListItemResponse> content,
    String nextCursor,
    boolean hasNext
) {

  public OfficeItemClaimSliceResponse {
    content = List.copyOf(content);
  }
}
