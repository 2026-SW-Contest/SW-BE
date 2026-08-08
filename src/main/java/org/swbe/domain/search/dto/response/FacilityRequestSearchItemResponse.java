package org.swbe.domain.search.dto.response;

import java.time.LocalDateTime;

public record FacilityRequestSearchItemResponse(
    Long facilityRequestId,
    String title,
    String description,
    String equipmentName,
    String categoryName,
    String locationName,
    String requestStatus,
    String requestStatusName,
    String thumbnailUrl,
    LocalDateTime createdAt
) {
}
