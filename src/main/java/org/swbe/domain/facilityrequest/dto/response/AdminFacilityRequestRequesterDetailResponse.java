package org.swbe.domain.facilityrequest.dto.response;

public record AdminFacilityRequestRequesterDetailResponse(
    Long userId,
    String name,
    String studentNumber,
    String email
) {
}
