package org.swbe.domain.search.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SearchSuggestionQueryRepositoryTest {

  @Test
  void domainQueriesBindPatternsAndLimitResultsSeparately() {
    EntityManager entityManager = mock(EntityManager.class);
    Query query = mock(Query.class);
    when(entityManager.createNativeQuery(anyString()))
        .thenReturn(query);
    when(query.getResultList())
        .thenReturn(List.of("에어팟 프로"))
        .thenReturn(List.of("에어컨 소음 점검 요청"));
    SearchSuggestionQueryRepository repository =
        new SearchSuggestionQueryRepository(entityManager);

    List<String> lostItemSuggestions =
        repository.findLostItemSuggestions(
            "에어",
            "%에어%",
            "에어%",
            5
        );
    List<String> facilityRequestSuggestions =
        repository.findFacilityRequestSuggestions(
            "에어",
            "%에어%",
            "에어%",
            5
        );

    assertThat(lostItemSuggestions)
        .containsExactly("에어팟 프로");
    assertThat(facilityRequestSuggestions)
        .containsExactly("에어컨 소음 점검 요청");
    verify(query, times(2))
        .setParameter("normalizedQuery", "에어");
    verify(query, times(2))
        .setParameter("containsPattern", "%에어%");
    verify(query, times(2))
        .setParameter("prefixPattern", "에어%");
    verify(query, times(2)).setMaxResults(5);

    ArgumentCaptor<String> sqlCaptor =
        ArgumentCaptor.forClass(String.class);
    verify(entityManager, times(2))
        .createNativeQuery(sqlCaptor.capture());
    assertThat(sqlCaptor.getAllValues().get(0))
        .contains("item.item_name")
        .doesNotContain("facility_request");
    assertThat(sqlCaptor.getAllValues().get(1))
        .contains("request.title")
        .doesNotContain("stored_item");
  }
}
