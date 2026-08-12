package org.swbe.domain.facilityrequest.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AdminFacilityRequestDetailDataResponse(
    Long facilityRequestId,
    String title,
    String description,
    AdminFacilityRequestRequesterDetailResponse requester,
    FacilityCategoryResponse category,
    AdminFacilityRequestLocationResponse location,
    String requestStatus,
    String requestStatusName,
    List<FacilityRequestAttachmentResponse> attachments,
    List<AdminFacilityRequestAdminResponse> adminResponses,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

  public AdminFacilityRequestDetailDataResponse {
    attachments = List.copyOf(attachments);
    adminResponses = List.copyOf(adminResponses);
  }
}
