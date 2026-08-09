package org.swbe.domain.search.dto.response;

import java.util.List;

public record RecentSearchListResponse(
    List<RecentSearchResponse> data
) {

  public RecentSearchListResponse {
    data = List.copyOf(data);
  }
}
