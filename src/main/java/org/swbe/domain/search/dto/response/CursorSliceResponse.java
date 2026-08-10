package org.swbe.domain.search.dto.response;

import java.util.List;

public record CursorSliceResponse<T>(
    List<T> content,
    String nextCursor,
    boolean hasNext
) {

  public CursorSliceResponse {
    content = List.copyOf(content);
  }
}
