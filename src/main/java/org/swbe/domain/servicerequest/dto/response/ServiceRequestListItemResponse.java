package org.swbe.domain.servicerequest.dto.response;

import java.time.LocalDateTime;

public record ServiceRequestListItemResponse(
    Long serviceRequestId,
    String title,
    String categoryName,
    String locationName,
    String requestStatus,
    String requestStatusName,
    String thumbnailUrl,
    LocalDateTime createdAt
) {
}
