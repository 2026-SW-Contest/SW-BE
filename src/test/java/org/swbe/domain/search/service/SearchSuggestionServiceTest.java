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
    when(repository.findSuggestions(
        "air",
        "%air%",
        "air%",
        8
    )).thenReturn(List.of("AirPods Pro"));
    SearchSuggestionService service =
        new SearchSuggestionService(repository);

    var response = service.getSuggestions(" AIR ", 8);

    assertThat(response.data())
        .containsExactly("AirPods Pro");
    verify(repository).findSuggestions(
        "air",
        "%air%",
        "air%",
        8
    );
  }
}
