package org.swbe.domain.lostitem.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.service.FilePublicUrlResolver;
import org.swbe.domain.lostitem.dto.response.StoredItemAttachmentResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemCategoryResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemDetailDataResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemDetailResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemLocationResponse;
import org.swbe.domain.lostitem.dto.response.MyItemClaimResultResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemOfficeResponse;
import org.swbe.domain.lostitem.entity.ItemClaim;
import org.swbe.domain.lostitem.entity.ItemClaimStatus;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemAttachment;
import org.swbe.domain.lostitem.exception.StoredItemErrorCode;
import org.swbe.domain.lostitem.repository.StoredItemAttachmentRepository;
import org.swbe.domain.lostitem.repository.ItemClaimRepository;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.global.error.BusinessException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoredItemDetailService {

  private final StoredItemRepository storedItemRepository;
  private final StoredItemAttachmentRepository attachmentRepository;
  private final ItemClaimRepository itemClaimRepository;
  private final FilePublicUrlResolver filePublicUrlResolver;

  public StoredItemDetailResponse getStoredItem(
      Long storedItemId,
      Long viewerUserId
  ) {
    StoredItem item = storedItemRepository.findDetailById(storedItemId)
        .orElseThrow(this::notFound);
    List<StoredItemAttachmentResponse> attachments = attachmentRepository
        .findPublicImagesByStoredItemId(storedItemId)
        .stream()
        .map(this::toAttachmentResponse)
        .toList();
    String foundLocationName = item.getFoundLocationName();

    return new StoredItemDetailResponse(
        new StoredItemDetailDataResponse(
            item.getId(),
            item.getItemName(),
            item.getPublicDescription(),
            new StoredItemCategoryResponse(
                item.getItemCategory().getId(),
                item.getItemCategory().getName()
            ),
            foundLocationName == null
                ? null
                : new StoredItemLocationResponse(
                    item.getFoundLocation() == null
                        ? null
                        : item.getFoundLocation().getId(),
                    foundLocationName
                ),
            item.getFoundDate(),
            item.getPublicStatus().name(),
            item.getPublicStatus().getDisplayName(),
            new StoredItemOfficeResponse(
                item.getOffice().getId(),
                item.getOffice().getName()
            ),
            attachments,
            resolveMyClaimResult(storedItemId, viewerUserId),
            item.getCreatedAt(),
            item.getUpdatedAt()
        )
    );
  }

  private MyItemClaimResultResponse resolveMyClaimResult(
      Long storedItemId,
      Long viewerUserId
  ) {
    if (viewerUserId == null) {
      return null;
    }
    return itemClaimRepository
        .findFirstByStoredItem_IdAndClaimantUser_IdOrderByCreatedAtDescIdDesc(
            storedItemId,
            viewerUserId
        )
        .filter(claim -> claim.getClaimStatus().isDecision())
        .map(this::toMyClaimResult)
        .orElse(null);
  }

  private MyItemClaimResultResponse toMyClaimResult(ItemClaim claim) {
    ItemClaimStatus decision = claim.getClaimStatus();
    return new MyItemClaimResultResponse(
        claim.getId(),
        decision.name(),
        decision.getDisplayName(),
        claim.getDecisionMessage(),
        claim.getDecidedAt()
    );
  }

  private StoredItemAttachmentResponse toAttachmentResponse(
      StoredItemAttachment attachment
  ) {
    FileResource file = attachment.getFile();
    return new StoredItemAttachmentResponse(
        file.getId(),
        file.getOriginalFilename(),
        filePublicUrlResolver.resolve(file)
    );
  }

  private BusinessException notFound() {
    return new BusinessException(StoredItemErrorCode.NOT_FOUND);
  }
}
