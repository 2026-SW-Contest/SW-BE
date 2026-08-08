package org.swbe.domain.search.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LostItemSearchItemResponse(
    Long storedItemId,
    String itemName,
    String categoryName,
    String description,
    String foundLocationName,
    LocalDate foundDate,
    String publicStatus,
    String thumbnailUrl,
    LocalDateTime createdAt
) {
}
