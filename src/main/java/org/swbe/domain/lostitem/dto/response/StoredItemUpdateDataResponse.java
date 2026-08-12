package org.swbe.domain.lostitem.dto.response;

import java.time.LocalDateTime;

public record StoredItemUpdateDataResponse(
    Long storedItemId,
    String publicStatus,
    int attachmentCount,
    LocalDateTime updatedAt
) {
}
