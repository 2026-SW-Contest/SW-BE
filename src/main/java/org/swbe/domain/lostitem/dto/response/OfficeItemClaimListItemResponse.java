package org.swbe.domain.lostitem.dto.response;

import java.time.LocalDateTime;

public record OfficeItemClaimListItemResponse(
    Long itemClaimId,
    Long storedItemId,
    String itemName,
    String claimantName,
    String studentNumber,
    String requestMethod,
    String claimStatus,
    String claimStatusName,
    String thumbnailUrl,
    int attachmentCount,
    LocalDateTime createdAt
) {
}
