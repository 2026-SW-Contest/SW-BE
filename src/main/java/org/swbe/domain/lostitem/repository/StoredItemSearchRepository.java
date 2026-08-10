package org.swbe.domain.lostitem.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.swbe.domain.lostitem.entity.StoredItem;

public interface StoredItemSearchRepository {

  long countSearchMatches(String pattern);

  List<StoredItem> searchByCursor(
      String pattern,
      LocalDateTime cursorCreatedAt,
      Long cursorId,
      Pageable pageable
  );
}
