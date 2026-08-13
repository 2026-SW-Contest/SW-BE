package org.swbe.domain.lostitem.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.service.PrivateFileUrlResolver;
import org.swbe.domain.lostitem.dto.request.ItemClaimSearchCondition;
import org.swbe.domain.lostitem.dto.response.ClaimStatusHistoryResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimAttachmentResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimDetailDataResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimDetailResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimListItemResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimListResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimPageResponse;
import org.swbe.domain.lostitem.dto.response.OfficeItemClaimListItemResponse;
import org.swbe.domain.lostitem.dto.response.OfficeItemClaimListResponse;
import org.swbe.domain.lostitem.dto.response.OfficeItemClaimPageResponse;
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
import org.swbe.domain.lostitem.repository.LostItemOfficeRepository;
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
  private final LostItemOfficeRepository officeRepository;
  private final OfficeStaffAssignmentRepository assignmentRepository;
  private final ItemClaimThumbnailService thumbnailService;
  private final PrivateFileUrlResolver privateFileUrlResolver;

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
    validateOfficeAccess(
        storedItem.getOffice().getId(),
        requesterUserId,
        admin
    );

    Page<ItemClaim> result = itemClaimRepository.findAllByStoredItemId(
        storedItemId,
        condition.status(),
        PageRequest.of(condition.page(), condition.size())
    );
    List<ItemClaim> content = result.getContent();
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

    return new ItemClaimListResponse(new ItemClaimPageResponse(
        responses,
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages(),
        result.hasNext()
    ));
  }

  public OfficeItemClaimListResponse getOfficeItemClaims(
      Long officeId,
      ItemClaimSearchCondition condition,
      Long requesterUserId,
      boolean admin
  ) {
    if (!officeRepository.existsById(officeId)) {
      throw new BusinessException(ItemClaimErrorCode.OFFICE_NOT_FOUND);
    }
    validateOfficeAccess(officeId, requesterUserId, admin);

    Page<ItemClaim> result = itemClaimRepository
        .findAllByOfficeId(
            officeId,
            condition.status(),
            PageRequest.of(condition.page(), condition.size())
        );
    List<ItemClaim> content = result.getContent();
    Map<Long, ItemClaimAttachmentSummary> summaries = content.isEmpty()
        ? Map.of()
        : thumbnailService.resolveAll(
            content.stream().map(ItemClaim::getId).toList()
        );
    List<OfficeItemClaimListItemResponse> responses = content.stream()
        .map(claim -> toOfficeListItemResponse(
            claim,
            summaries.get(claim.getId())
        ))
        .toList();

    return new OfficeItemClaimListResponse(
        new OfficeItemClaimPageResponse(
            responses,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.hasNext()
        )
    );
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
        claim.getStoredItem().getOffice().getId(),
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

  private OfficeItemClaimListItemResponse toOfficeListItemResponse(
      ItemClaim claim,
      ItemClaimAttachmentSummary summary
  ) {
    ItemClaimStatus status = claim.getClaimStatus();
    StoredItem storedItem = claim.getStoredItem();
    return new OfficeItemClaimListItemResponse(
        claim.getId(),
        storedItem.getId(),
        storedItem.getItemName(),
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
      Long officeId,
      Long requesterUserId,
      boolean admin
  ) {
    if (!admin && !assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(
            officeId,
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

}
