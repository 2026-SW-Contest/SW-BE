package org.swbe.domain.lostitem.dto.response;

public record LostItemOfficeResponse(
    Long officeId,
    String officeName,
    Long buildingId,
    String buildingCode,
    String buildingName,
    Long locationId,
    String locationName,
    String floor,
    String departmentName,
    String operatingHours,
    String guidance,
    boolean primary
) {
}
