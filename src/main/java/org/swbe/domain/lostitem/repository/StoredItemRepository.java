package org.swbe.domain.lostitem.repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.swbe.domain.lostitem.entity.StoredItem;

public interface StoredItemRepository
    extends JpaRepository<StoredItem, Long>,
    StoredItemSearchRepository,
    StoredItemQueryRepository {

  @EntityGraph(attributePaths = {
      "itemCategory",
      "foundLocation",
      "office"
  })
  @Query("SELECT item FROM StoredItem item WHERE item.id = :id")
  Optional<StoredItem> findDetailById(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT item FROM StoredItem item WHERE item.id = :id")
  Optional<StoredItem> findByIdForUpdate(@Param("id") Long id);
}
