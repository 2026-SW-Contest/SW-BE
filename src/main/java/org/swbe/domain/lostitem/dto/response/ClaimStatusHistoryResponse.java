package org.swbe.domain.lostitem.dto.response;

import java.time.LocalDateTime;

public record ClaimStatusHistoryResponse(
    Long claimStatusHistoryId,
    String previousStatus,
    String previousStatusName,
    String newStatus,
    String newStatusName,
    String changedByName,
    String changeReason,
    LocalDateTime changedAt
) {
}
