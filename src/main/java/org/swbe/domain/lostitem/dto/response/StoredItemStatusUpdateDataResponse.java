package org.swbe.domain.lostitem.dto.response;

import java.time.LocalDateTime;

public record StoredItemStatusUpdateDataResponse(
    Long storedItemId,
    String previousStatus,
    String publicStatus,
    String publicStatusName,
    boolean changed,
    LocalDateTime changedAt
) {
}
