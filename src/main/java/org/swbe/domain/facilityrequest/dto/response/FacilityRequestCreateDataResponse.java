package org.swbe.domain.facilityrequest.dto.response;

import java.time.LocalDateTime;

public record FacilityRequestCreateDataResponse(
    Long facilityRequestId,
    String receiptNumber,
    String requestStatus,
    int attachmentCount,
    LocalDateTime createdAt
) {
}
