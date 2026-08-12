package org.swbe.domain.lostitem.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StoredItemListItemResponse(
    Long storedItemId,
    String itemName,
    String description,
    String categoryName,
    String foundLocationName,
    LocalDate foundDate,
    String publicStatus,
    String publicStatusName,
    String thumbnailUrl,
    LocalDateTime createdAt
) {
}
