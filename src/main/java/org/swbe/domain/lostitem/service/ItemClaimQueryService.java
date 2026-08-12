package org.swbe.domain.lostitem.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.service.PrivateFileUrlResolver;
import org.swbe.domain.lostitem.cursor.ItemClaimCursor;
import org.swbe.domain.lostitem.cursor.ItemClaimCursorCodec;
import org.swbe.domain.lostitem.dto.request.ItemClaimSearchCondition;
import org.swbe.domain.lostitem.dto.response.ClaimStatusHistoryResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimAttachmentResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimDetailDataResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimDetailResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimListItemResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimListResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimSliceResponse;
import org.swbe.domain.lostitem.entity.ClaimStatusHistory;
import org.swbe.domain.lostitem.entity.ItemClaim;
import org.swbe.domain.lostitem.entity.ItemClaimAttachment;
import org.swbe.domain.lostitem.entity.ItemClaimStatus;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.exception.ItemClaimErrorCode;
import org.swbe.domain.lostitem.exception.StoredItemErrorCode;
import org.swbe.domain.lostitem.repository.ClaimStatusHistoryRepository;
import org.swbe.domain.lostitem.repository.ItemClaimAttachmentRepository;
import org.swbe.domain.lostitem.repository.ItemClaimRepository;
import org.swbe.domain.lostitem.repository.OfficeStaffAssignmentRepository;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.global.error.BusinessException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemClaimQueryService {

  private final StoredItemRepository storedItemRepository;
  private final ItemClaimRepository itemClaimRepository;
  private final ItemClaimAttachmentRepository attachmentRepository;
  private final ClaimStatusHistoryRepository historyRepository;
  private final OfficeStaffAssignmentRepository assignmentRepository;
  private final ItemClaimThumbnailService thumbnailService;
  private final PrivateFileUrlResolver privateFileUrlResolver;
  private final ItemClaimCursorCodec cursorCodec;

  public ItemClaimListResponse getItemClaims(
      Long storedItemId,
      ItemClaimSearchCondition condition,
      Long requesterUserId,
      boolean admin
  ) {
    StoredItem storedItem = storedItemRepository
        .findDetailById(storedItemId)
        .orElseThrow(() -> new BusinessException(
            StoredItemErrorCode.NOT_FOUND
        ));
    validateOfficeAccess(storedItem, requesterUserId, admin);

    ItemClaimCursor cursor = condition.cursor() == null
        ? null
        : cursorCodec.decode(condition.cursor());
    List<ItemClaim> matches = itemClaimRepository.findAllByCursor(
        storedItemId,
        condition.status(),
        cursor == null ? null : cursor.createdAt(),
        cursor == null ? null : cursor.id(),
        PageRequest.of(0, condition.size() + 1)
    );
    boolean hasNext = matches.size() > condition.size();
    List<ItemClaim> content = hasNext
        ? matches.subList(0, condition.size())
        : matches;
    Map<Long, ItemClaimAttachmentSummary> summaries = content.isEmpty()
        ? Map.of()
        : thumbnailService.resolveAll(
            content.stream().map(ItemClaim::getId).toList()
        );
    List<ItemClaimListItemResponse> responses = content.stream()
        .map(claim -> toListItemResponse(
            claim,
            summaries.get(claim.getId())
        ))
        .toList();

    return new ItemClaimListResponse(new ItemClaimSliceResponse(
        responses,
        nextCursor(content, hasNext),
        hasNext
    ));
  }

  public ItemClaimDetailResponse getItemClaim(
      Long itemClaimId,
      Long requesterUserId,
      boolean admin
  ) {
    ItemClaim claim = itemClaimRepository.findDetailById(itemClaimId)
        .orElseThrow(() -> new BusinessException(
            ItemClaimErrorCode.NOT_FOUND
        ));
    validateOfficeAccess(
        claim.getStoredItem(),
        requesterUserId,
        admin
    );
    List<ItemClaimAttachmentResponse> attachments = attachmentRepository
        .findPublicImagesByItemClaimId(itemClaimId)
        .stream()
        .map(this::toAttachmentResponse)
        .toList();
    List<ClaimStatusHistoryResponse> histories = historyRepository
        .findAllByItemClaimId(itemClaimId)
        .stream()
        .map(this::toHistoryResponse)
        .toList();
    ItemClaimStatus status = claim.getClaimStatus();

    return new ItemClaimDetailResponse(
        new ItemClaimDetailDataResponse(
            claim.getId(),
            claim.getStoredItem().getId(),
            claimantName(claim),
            studentNumber(claim),
            claim.getRequestMethod(),
            claim.getOwnershipDescription(),
            status.name(),
            status.getDisplayName(),
            attachments,
            histories,
            claim.getCreatedAt(),
            claim.getUpdatedAt()
        )
    );
  }

  private ItemClaimListItemResponse toListItemResponse(
      ItemClaim claim,
      ItemClaimAttachmentSummary summary
  ) {
    ItemClaimStatus status = claim.getClaimStatus();
    return new ItemClaimListItemResponse(
        claim.getId(),
        claimantName(claim),
        studentNumber(claim),
        claim.getRequestMethod(),
        status.name(),
        status.getDisplayName(),
        summary == null ? null : summary.thumbnailUrl(),
        summary == null ? 0 : summary.attachmentCount(),
        claim.getCreatedAt()
    );
  }

  private ItemClaimAttachmentResponse toAttachmentResponse(
      ItemClaimAttachment attachment
  ) {
    FileResource file = attachment.getFile();
    return new ItemClaimAttachmentResponse(
        file.getId(),
        file.getOriginalFilename(),
        privateFileUrlResolver.resolve(file)
    );
  }

  private ClaimStatusHistoryResponse toHistoryResponse(
      ClaimStatusHistory history
  ) {
    ItemClaimStatus previousStatus = history.getPreviousStatus();
    ItemClaimStatus newStatus = history.getNewStatus();
    return new ClaimStatusHistoryResponse(
        history.getId(),
        previousStatus == null ? null : previousStatus.name(),
        previousStatus == null ? null : previousStatus.getDisplayName(),
        newStatus.name(),
        newStatus.getDisplayName(),
        history.getChangedBy() == null
            ? null
            : history.getChangedBy().getName(),
        history.getChangeReason(),
        history.getChangedAt()
    );
  }

  private void validateOfficeAccess(
      StoredItem storedItem,
      Long requesterUserId,
      boolean admin
  ) {
    if (!admin && !assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(
            storedItem.getOffice().getId(),
            requesterUserId
        )) {
      throw new BusinessException(ItemClaimErrorCode.ACCESS_DENIED);
    }
  }

  private String claimantName(ItemClaim claim) {
    return claim.getClaimantUser() == null
        ? claim.getTemporaryClaimant().getName()
        : claim.getClaimantUser().getName();
  }

  private String studentNumber(ItemClaim claim) {
    return claim.getClaimantUser() == null
        ? claim.getTemporaryClaimant().getStudentNumber()
        : claim.getClaimantUser().getStudentNumber();
  }

  private String nextCursor(
      List<ItemClaim> content,
      boolean hasNext
  ) {
    if (!hasNext) {
      return null;
    }
    ItemClaim last = content.getLast();
    return cursorCodec.encode(last.getCreatedAt(), last.getId());
  }
}
