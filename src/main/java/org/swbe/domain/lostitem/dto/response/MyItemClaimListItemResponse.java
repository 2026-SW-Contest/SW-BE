package org.swbe.domain.lostitem.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MyItemClaimListItemResponse(
    Long itemClaimId,
    Long storedItemId,
    String itemName,
    String categoryName,
    String foundLocationName,
    LocalDate foundDate,
    String requestMethod,
    String claimStatus,
    String claimStatusName,
    String thumbnailUrl,
    String decisionMessage,
    LocalDateTime createdAt,
    LocalDateTime decidedAt
) {
}
