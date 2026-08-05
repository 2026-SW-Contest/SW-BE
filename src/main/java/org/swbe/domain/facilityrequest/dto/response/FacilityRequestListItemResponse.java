package org.swbe.domain.facilityrequest.dto.response;

import java.time.LocalDateTime;

public record FacilityRequestListItemResponse(
    Long facilityRequestId,
    String title,
    String categoryName,
    String locationName,
    String requestStatus,
    String requestStatusName,
    String thumbnailUrl,
    LocalDateTime createdAt
) {
}
