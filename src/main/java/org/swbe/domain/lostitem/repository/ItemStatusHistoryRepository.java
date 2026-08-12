package org.swbe.domain.lostitem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.lostitem.entity.ItemStatusHistory;

public interface ItemStatusHistoryRepository
    extends JpaRepository<ItemStatusHistory, Long> {
}
