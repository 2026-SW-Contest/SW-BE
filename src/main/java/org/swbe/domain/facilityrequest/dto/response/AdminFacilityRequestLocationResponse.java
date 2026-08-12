package org.swbe.domain.facilityrequest.dto.response;

public record AdminFacilityRequestLocationResponse(
    Long locationId,
    String locationCode,
    String locationName
) {
}
