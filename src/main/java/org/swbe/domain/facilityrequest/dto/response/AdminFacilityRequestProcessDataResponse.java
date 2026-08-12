package org.swbe.domain.facilityrequest.dto.response;

import java.time.LocalDateTime;

public record AdminFacilityRequestProcessDataResponse(
    Long facilityRequestId,
    String previousStatus,
    String requestStatus,
    String requestStatusName,
    AdminFacilityRequestAdminResponse adminResponse,
    LocalDateTime updatedAt
) {
}
