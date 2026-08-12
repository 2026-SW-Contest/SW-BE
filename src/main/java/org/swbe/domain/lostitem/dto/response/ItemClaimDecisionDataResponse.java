package org.swbe.domain.lostitem.dto.response;

import java.time.LocalDateTime;

public record ItemClaimDecisionDataResponse(
    Long itemClaimId,
    Long storedItemId,
    String decision,
    String decisionName,
    String message,
    LocalDateTime decidedAt
) {
}
