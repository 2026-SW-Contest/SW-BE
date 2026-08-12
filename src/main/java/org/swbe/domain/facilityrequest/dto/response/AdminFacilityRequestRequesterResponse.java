package org.swbe.domain.facilityrequest.dto.response;

public record AdminFacilityRequestRequesterResponse(
    Long userId,
    String name,
    String studentNumber
) {
}
