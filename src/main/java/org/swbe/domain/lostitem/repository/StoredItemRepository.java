package org.swbe.domain.lostitem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.lostitem.entity.StoredItem;

public interface StoredItemRepository
    extends JpaRepository<StoredItem, Long>,
    StoredItemSearchRepository {
}
