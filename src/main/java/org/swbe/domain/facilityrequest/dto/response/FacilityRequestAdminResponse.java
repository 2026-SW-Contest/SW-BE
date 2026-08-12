package org.swbe.domain.facilityrequest.dto.response;

import java.time.LocalDateTime;

public record FacilityRequestAdminResponse(
    Long responseId,
    String content,
    LocalDateTime createdAt
) {
}
