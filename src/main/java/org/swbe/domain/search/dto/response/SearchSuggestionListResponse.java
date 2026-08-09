package org.swbe.domain.search.dto.response;

import java.util.List;

public record SearchSuggestionListResponse(
    List<String> data
) {

  public SearchSuggestionListResponse {
    data = List.copyOf(data);
  }
}
