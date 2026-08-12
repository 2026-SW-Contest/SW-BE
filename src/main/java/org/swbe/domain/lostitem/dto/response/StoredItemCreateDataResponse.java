package org.swbe.domain.lostitem.dto.response;

import java.time.LocalDateTime;

public record StoredItemCreateDataResponse(
    Long storedItemId,
    String publicStatus,
    int attachmentCount,
    LocalDateTime createdAt
) {
}
