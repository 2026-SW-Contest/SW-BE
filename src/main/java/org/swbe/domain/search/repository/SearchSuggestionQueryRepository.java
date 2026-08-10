package org.swbe.domain.search.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SearchSuggestionQueryRepository {

  private static final String LOST_ITEM_SUGGESTION_QUERY = """
      SELECT item.item_name AS candidate
      FROM stored_item item
      WHERE LOWER(item.item_name)
          LIKE :containsPattern ESCAPE '!'
      GROUP BY item.item_name
      ORDER BY
        CASE
          WHEN LOWER(item.item_name) = :normalizedQuery THEN 0
          WHEN LOWER(item.item_name)
              LIKE :prefixPattern ESCAPE '!' THEN 1
          ELSE 2
        END,
        COUNT(*) DESC,
        MAX(item.created_at) DESC,
        item.item_name
      """;

  private static final String FACILITY_REQUEST_SUGGESTION_QUERY = """
      SELECT request.title AS candidate
      FROM facility_request request
      WHERE LOWER(request.title)
          LIKE :containsPattern ESCAPE '!'
      GROUP BY request.title
      ORDER BY
        CASE
          WHEN LOWER(request.title) = :normalizedQuery THEN 0
          WHEN LOWER(request.title)
              LIKE :prefixPattern ESCAPE '!' THEN 1
          ELSE 2
        END,
        COUNT(*) DESC,
        MAX(request.created_at) DESC,
        request.title
      """;

  private final EntityManager entityManager;

  public List<String> findLostItemSuggestions(
      String normalizedQuery,
      String containsPattern,
      String prefixPattern,
      int size
  ) {
    return findSuggestions(
        LOST_ITEM_SUGGESTION_QUERY,
        normalizedQuery,
        containsPattern,
        prefixPattern,
        size
    );
  }

  public List<String> findFacilityRequestSuggestions(
      String normalizedQuery,
      String containsPattern,
      String prefixPattern,
      int size
  ) {
    return findSuggestions(
        FACILITY_REQUEST_SUGGESTION_QUERY,
        normalizedQuery,
        containsPattern,
        prefixPattern,
        size
    );
  }

  @SuppressWarnings("unchecked")
  private List<String> findSuggestions(
      String sql,
      String normalizedQuery,
      String containsPattern,
      String prefixPattern,
      int size
  ) {
    Query query = entityManager.createNativeQuery(
        sql
    );
    query.setParameter("normalizedQuery", normalizedQuery);
    query.setParameter("containsPattern", containsPattern);
    query.setParameter("prefixPattern", prefixPattern);
    query.setMaxResults(size);

    return ((List<Object>) query.getResultList()).stream()
        .map(String.class::cast)
        .toList();
  }
}
