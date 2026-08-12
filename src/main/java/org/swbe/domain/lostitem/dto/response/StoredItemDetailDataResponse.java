package org.swbe.domain.lostitem.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record StoredItemDetailDataResponse(
    Long storedItemId,
    String itemName,
    String description,
    StoredItemCategoryResponse category,
    StoredItemLocationResponse foundLocation,
    LocalDate foundDate,
    String publicStatus,
    String publicStatusName,
    StoredItemOfficeResponse office,
    List<StoredItemAttachmentResponse> attachments,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

  public StoredItemDetailDataResponse {
    attachments = List.copyOf(attachments);
  }
}
