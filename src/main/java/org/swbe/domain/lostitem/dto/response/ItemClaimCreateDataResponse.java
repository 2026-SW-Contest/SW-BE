package org.swbe.domain.lostitem.dto.response;

import java.time.LocalDateTime;

public record ItemClaimCreateDataResponse(
    Long itemClaimId,
    Long storedItemId,
    String claimantName,
    String studentNumber,
    String claimStatus,
    int attachmentCount,
    LocalDateTime createdAt
) {
}
