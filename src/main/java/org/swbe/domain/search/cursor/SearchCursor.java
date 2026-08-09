package org.swbe.domain.search.cursor;

import java.time.LocalDateTime;

public record SearchCursor(
    LocalDateTime createdAt,
    Long id
) {
}
