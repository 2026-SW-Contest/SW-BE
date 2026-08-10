package org.swbe.domain.search.dto.response;

import java.util.List;

public record SearchSuggestionDataResponse(
    List<String> lostItemSuggestions,
    List<String> facilityRequestSuggestions
) {

  public SearchSuggestionDataResponse {
    lostItemSuggestions = List.copyOf(lostItemSuggestions);
    facilityRequestSuggestions = List.copyOf(
        facilityRequestSuggestions
    );
  }
}
