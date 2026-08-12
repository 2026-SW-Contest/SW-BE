package org.swbe.domain.facilityrequest.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record FacilityRequestDetailDataResponse(
    Long facilityRequestId,
    String title,
    String description,
    FacilityCategoryResponse category,
    FacilityRequestLocationDetailResponse location,
    String requestStatus,
    String requestStatusName,
    List<FacilityRequestAttachmentResponse> attachments,
    boolean ownedByCurrentUser,
    boolean editable,
    boolean deletable,
    List<FacilityRequestAdminResponse> adminResponses,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

  public FacilityRequestDetailDataResponse {
    attachments = List.copyOf(attachments);
    adminResponses = List.copyOf(adminResponses);
  }
}
