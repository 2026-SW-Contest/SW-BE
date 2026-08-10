package org.swbe.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.swbe.domain.search.repository.SearchSuggestionQueryRepository;

class SearchSuggestionServiceTest {

  @Test
  void queryIsNormalizedBeforeSuggestionsAreRetrieved() {
    SearchSuggestionQueryRepository repository =
        mock(SearchSuggestionQueryRepository.class);
    when(repository.findLostItemSuggestions(
        "air",
        "%air%",
        "air%",
        5
    )).thenReturn(List.of("AirPods Pro"));
    when(repository.findFacilityRequestSuggestions(
        "air",
        "%air%",
        "air%",
        5
    )).thenReturn(List.of("Air conditioner inspection"));
    SearchSuggestionService service =
        new SearchSuggestionService(repository);

    var response = service.getSuggestions(" AIR ", 5);

    assertThat(response.data().lostItemSuggestions())
        .containsExactly("AirPods Pro");
    assertThat(response.data().facilityRequestSuggestions())
        .containsExactly("Air conditioner inspection");
    verify(repository).findLostItemSuggestions(
        "air",
        "%air%",
        "air%",
        5
    );
    verify(repository).findFacilityRequestSuggestions(
        "air",
        "%air%",
        "air%",
        5
    );
  }
}
