package org.swbe.domain.search.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SearchSuggestionQueryRepository {

  private static final String SUGGESTION_QUERY = """
      SELECT candidate
      FROM (
        SELECT
          item.item_name AS candidate,
          0 AS source_priority,
          item.created_at AS created_at
        FROM stored_item item
        WHERE LOWER(item.item_name)
            LIKE :containsPattern ESCAPE '!'

        UNION ALL

        SELECT
          request.equipment_name AS candidate,
          0 AS source_priority,
          request.created_at AS created_at
        FROM facility_request request
        WHERE request.visibility = 'PUBLIC'
          AND request.equipment_name IS NOT NULL
          AND LOWER(request.equipment_name)
              LIKE :containsPattern ESCAPE '!'

        UNION ALL

        SELECT
          request.title AS candidate,
          1 AS source_priority,
          request.created_at AS created_at
        FROM facility_request request
        WHERE request.visibility = 'PUBLIC'
          AND LOWER(request.title)
              LIKE :containsPattern ESCAPE '!'
      ) candidates
      GROUP BY candidate
      ORDER BY
        CASE
          WHEN LOWER(candidate) = :normalizedQuery THEN 0
          WHEN LOWER(candidate)
              LIKE :prefixPattern ESCAPE '!' THEN 1
          ELSE 2
        END,
        MIN(source_priority),
        COUNT(*) DESC,
        MAX(created_at) DESC,
        candidate
      """;

  private final EntityManager entityManager;

  @SuppressWarnings("unchecked")
  public List<String> findSuggestions(
      String normalizedQuery,
      String containsPattern,
      String prefixPattern,
      int size
  ) {
    Query query = entityManager.createNativeQuery(
        SUGGESTION_QUERY
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
