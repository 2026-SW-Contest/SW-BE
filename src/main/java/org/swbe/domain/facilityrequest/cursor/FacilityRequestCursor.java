package org.swbe.domain.facilityrequest.cursor;

import java.time.LocalDateTime;

public record FacilityRequestCursor(
    LocalDateTime createdAt,
    Long id
) {
}
