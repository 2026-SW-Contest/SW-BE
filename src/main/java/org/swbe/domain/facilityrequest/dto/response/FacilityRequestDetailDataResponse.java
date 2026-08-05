package org.swbe.domain.facilityrequest.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record FacilityRequestDetailDataResponse(
    Long facilityRequestId,
    String receiptNumber,
    String title,
    String description,
    String equipmentName,
    FacilityCategoryResponse category,
    FacilityRequestLocationDetailResponse location,
    String requestStatus,
    String requestStatusName,
    List<FacilityRequestAttachmentResponse> attachments,
    boolean editable,
    boolean deletable,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

  public FacilityRequestDetailDataResponse {
    attachments = List.copyOf(attachments);
  }
}
