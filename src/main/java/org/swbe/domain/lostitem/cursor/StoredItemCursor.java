package org.swbe.domain.lostitem.cursor;

import java.time.LocalDateTime;

public record StoredItemCursor(
    LocalDateTime createdAt,
    Long id
) {
}
