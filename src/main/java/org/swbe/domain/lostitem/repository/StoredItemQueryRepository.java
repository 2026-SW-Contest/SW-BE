package org.swbe.domain.lostitem.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemStatus;

public interface StoredItemQueryRepository {

  List<StoredItem> findAllByCursor(
      Long categoryId,
      Long locationId,
      StoredItemStatus status,
      LocalDate from,
      LocalDate to,
      LocalDateTime cursorCreatedAt,
      Long cursorId,
      int limit
  );
}
