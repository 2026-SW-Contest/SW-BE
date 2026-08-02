package org.swbe.domain.servicerequest.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ServiceRequestDetailDataResponse(
    Long serviceRequestId,
    String receiptNumber,
    String title,
    String description,
    String equipmentName,
    ServiceRequestCategoryDetailResponse category,
    ServiceRequestLocationDetailResponse location,
    String requestStatus,
    String requestStatusName,
    List<ServiceRequestAttachmentResponse> attachments,
    boolean editable,
    boolean deletable,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

  public ServiceRequestDetailDataResponse {
    attachments = List.copyOf(attachments);
  }
}
