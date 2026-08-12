package org.swbe.domain.lostitem.dto.response;

import java.time.LocalDateTime;

public record MyItemClaimResultResponse(
    Long itemClaimId,
    String decision,
    String decisionName,
    String message,
    LocalDateTime decidedAt
) {
}
