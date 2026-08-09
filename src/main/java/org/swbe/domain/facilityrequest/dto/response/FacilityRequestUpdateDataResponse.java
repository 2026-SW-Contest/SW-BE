package org.swbe.domain.facilityrequest.dto.response;

import java.time.LocalDateTime;

public record FacilityRequestUpdateDataResponse(
    Long facilityRequestId,
    String requestStatus,
    int attachmentCount,
    LocalDateTime updatedAt
) {
}
