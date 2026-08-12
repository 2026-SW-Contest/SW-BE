package org.swbe.domain.lostitem.cursor;

import java.time.LocalDateTime;

public record ItemClaimCursor(
    LocalDateTime createdAt,
    Long id
) {
}
