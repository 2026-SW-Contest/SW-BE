package org.swbe.domain.lostitem.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.swbe.domain.lostitem.entity.StoredItem;

public interface StoredItemRepository
    extends JpaRepository<StoredItem, Long> {

  @Query("""
      SELECT COUNT(item)
      FROM StoredItem item
      JOIN item.itemCategory category
      LEFT JOIN item.foundLocation location
      WHERE
        LOWER(item.itemName) LIKE :pattern ESCAPE '!'
        OR LOWER(COALESCE(item.publicDescription, ''))
            LIKE :pattern ESCAPE '!'
        OR LOWER(category.name)
            LIKE :pattern ESCAPE '!'
        OR LOWER(COALESCE(location.name, ''))
            LIKE :pattern ESCAPE '!'
      """)
  long countSearchMatches(@Param("pattern") String pattern);

  @Query("""
      SELECT item
      FROM StoredItem item
      JOIN FETCH item.itemCategory category
      LEFT JOIN FETCH item.foundLocation location
      WHERE (
        LOWER(item.itemName) LIKE :pattern ESCAPE '!'
        OR LOWER(COALESCE(item.publicDescription, ''))
            LIKE :pattern ESCAPE '!'
        OR LOWER(category.name)
            LIKE :pattern ESCAPE '!'
        OR LOWER(COALESCE(location.name, ''))
            LIKE :pattern ESCAPE '!'
      )
      AND (
        :cursorCreatedAt IS NULL
        OR item.createdAt < :cursorCreatedAt
        OR (
          item.createdAt = :cursorCreatedAt
          AND item.id < :cursorId
        )
      )
      ORDER BY item.createdAt DESC, item.id DESC
      """)
  List<StoredItem> searchByCursor(
      @Param("pattern") String pattern,
      @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
      @Param("cursorId") Long cursorId,
      Pageable pageable
  );
}
