package org.swbe.domain.lostitem.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ItemClaimDetailDataResponse(
    Long itemClaimId,
    Long storedItemId,
    String claimantName,
    String studentNumber,
    String requestMethod,
    String ownershipDescription,
    String claimStatus,
    String claimStatusName,
    List<ItemClaimAttachmentResponse> attachments,
    List<ClaimStatusHistoryResponse> statusHistories,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

  public ItemClaimDetailDataResponse {
    attachments = List.copyOf(attachments);
    statusHistories = List.copyOf(statusHistories);
  }
}
