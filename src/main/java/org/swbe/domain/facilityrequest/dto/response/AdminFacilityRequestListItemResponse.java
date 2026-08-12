package org.swbe.domain.facilityrequest.dto.response;

import java.time.LocalDateTime;

public record AdminFacilityRequestListItemResponse(
    Long facilityRequestId,
    String title,
    AdminFacilityRequestRequesterResponse requester,
    FacilityCategoryResponse category,
    AdminFacilityRequestLocationResponse location,
    String requestStatus,
    String requestStatusName,
    String thumbnailUrl,
    LocalDateTime createdAt
) {
}
