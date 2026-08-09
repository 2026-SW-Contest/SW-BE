package org.swbe.domain.search.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SearchSuggestionQueryRepositoryTest {

  @Test
  void fixedNativeQueryBindsPatternsAndLimitsResults() {
    EntityManager entityManager = mock(EntityManager.class);
    Query query = mock(Query.class);
    when(entityManager.createNativeQuery(anyString()))
        .thenReturn(query);
    when(query.getResultList()).thenReturn(List.of(
        "에어팟 프로",
        "에어컨 소음 점검 요청"
    ));
    SearchSuggestionQueryRepository repository =
        new SearchSuggestionQueryRepository(entityManager);

    List<String> result = repository.findSuggestions(
        "에어",
        "%에어%",
        "에어%",
        8
    );

    assertThat(result).containsExactly(
        "에어팟 프로",
        "에어컨 소음 점검 요청"
    );
    verify(query).setParameter("normalizedQuery", "에어");
    verify(query).setParameter("containsPattern", "%에어%");
    verify(query).setParameter("prefixPattern", "에어%");
    verify(query).setMaxResults(8);

    ArgumentCaptor<String> sqlCaptor =
        ArgumentCaptor.forClass(String.class);
    verify(entityManager).createNativeQuery(sqlCaptor.capture());
    assertThat(sqlCaptor.getValue())
        .contains("UNION ALL")
        .contains("item.item_name")
        .contains("request.title")
        .doesNotContain("request.equipment_name")
        .doesNotContain("request.visibility");
  }
}
