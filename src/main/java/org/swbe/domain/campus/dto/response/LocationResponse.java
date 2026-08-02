package org.swbe.domain.campus.dto.response;

public record LocationResponse(
    Long locationId,
    String locationCode,
    String locationName
) {
}
