package org.swbe.domain.lostitem.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.lostitem.entity.ItemStatusHistory;

public interface ItemStatusHistoryRepository
    extends JpaRepository<ItemStatusHistory, Long> {

  List<ItemStatusHistory> findAllByStoredItem_IdOrderByIdAsc(
      Long storedItemId
  );
}
