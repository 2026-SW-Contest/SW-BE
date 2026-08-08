package org.swbe.domain.facilityrequest.dto.response;

import java.time.LocalDateTime;

public record FacilityRequestCreateDataResponse(
    Long facilityRequestId,
    String requestStatus,
    int attachmentCount,
    LocalDateTime createdAt
) {
}
