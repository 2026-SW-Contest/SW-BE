package org.swbe.domain.lostitem.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.repository.FileResourceRepository;
import org.swbe.domain.file.storage.FileStorageException;
import org.swbe.domain.file.storage.FileStorageRegistry;
import org.swbe.domain.file.storage.StoredFile;
import org.swbe.domain.lostitem.dto.request.ItemClaimCreateRequest;
import org.swbe.domain.lostitem.dto.response.ItemClaimCreateDataResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimCreateResponse;
import org.swbe.domain.lostitem.entity.ClaimStatusHistory;
import org.swbe.domain.lostitem.entity.ItemClaim;
import org.swbe.domain.lostitem.entity.ItemClaimAttachment;
import org.swbe.domain.lostitem.entity.ItemClaimStatus;
import org.swbe.domain.lostitem.entity.ItemStatusHistory;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemStatus;
import org.swbe.domain.lostitem.exception.ItemClaimErrorCode;
import org.swbe.domain.lostitem.exception.StoredItemErrorCode;
import org.swbe.domain.lostitem.repository.ClaimStatusHistoryRepository;
import org.swbe.domain.lostitem.repository.ItemClaimAttachmentRepository;
import org.swbe.domain.lostitem.repository.ItemClaimRepository;
import org.swbe.domain.lostitem.repository.ItemStatusHistoryRepository;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.exception.AuthErrorCode;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.global.error.BusinessException;

@Service
@RequiredArgsConstructor
public class ItemClaimCreateService {

  private static final int MAX_FILE_COUNT = 5;
  private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
      "image/jpeg",
      "image/png",
      "image/gif",
      "image/webp"
  );

  private final StoredItemRepository storedItemRepository;
  private final ItemClaimRepository itemClaimRepository;
  private final ItemClaimAttachmentRepository attachmentRepository;
  private final ClaimStatusHistoryRepository claimHistoryRepository;
  private final ItemStatusHistoryRepository itemHistoryRepository;
  private final AppUserRepository appUserRepository;
  private final FileResourceRepository fileResourceRepository;
  private final FileStorageRegistry fileStorageRegistry;
  private final Clock clock;

  @Transactional
  public ItemClaimCreateResponse create(
      Long storedItemId,
      ItemClaimCreateRequest request,
      List<MultipartFile> files,
      Long claimantUserId
  ) {
    List<MultipartFile> images = files == null ? List.of() : files;
    validateFiles(images);

    StoredItem storedItem = storedItemRepository
        .findByIdForUpdate(storedItemId)
        .orElseThrow(() -> new BusinessException(
            StoredItemErrorCode.NOT_FOUND
        ));
    validateClaimable(storedItem);
    AppUser claimant = appUserRepository.findById(claimantUserId)
        .orElseThrow(() -> new BusinessException(
            AuthErrorCode.ACCOUNT_NOT_FOUND
        ));
    validateNoActiveClaim(storedItemId, claimantUserId);

    LocalDateTime now = LocalDateTime.now(clock);
    ItemClaim claim = ItemClaim.createOnline(
        storedItem,
        claimant,
        request.ownershipDescription(),
        now
    );
    List<StoredFile> storedFiles = new ArrayList<>();
    try {
      itemClaimRepository.save(claim);
      claimHistoryRepository.save(ClaimStatusHistory.recordInitial(
          claim,
          claimant,
          now
      ));
      saveAttachments(
          claim,
          claimant,
          images,
          now,
          storedFiles
      );
      changeStoredItemToInProgress(storedItem, claimant, now);
      itemClaimRepository.flush();

      return new ItemClaimCreateResponse(
          new ItemClaimCreateDataResponse(
              claim.getId(),
              storedItem.getId(),
              claimant.getName(),
              claimant.getStudentNumber(),
              claim.getClaimStatus().name(),
              storedFiles.size(),
              claim.getCreatedAt()
          )
      );
    } catch (FileStorageException exception) {
      deleteStoredFiles(storedFiles);
      throw new BusinessException(ItemClaimErrorCode.FILE_STORAGE_ERROR);
    } catch (RuntimeException exception) {
      deleteStoredFiles(storedFiles);
      throw exception;
    }
  }

  private void validateClaimable(StoredItem storedItem) {
    if (storedItem.getPublicStatus() == StoredItemStatus.COMPLETED) {
      throw new BusinessException(ItemClaimErrorCode.NOT_CLAIMABLE);
    }
  }

  private void validateNoActiveClaim(
      Long storedItemId,
      Long claimantUserId
  ) {
    if (itemClaimRepository
        .existsByStoredItem_IdAndClaimantUser_IdAndClaimStatusIn(
            storedItemId,
            claimantUserId,
            ItemClaimStatus.activeStatuses()
        )) {
      throw new BusinessException(
          ItemClaimErrorCode.DUPLICATE_ACTIVE_CLAIM
      );
    }
  }

  private void changeStoredItemToInProgress(
      StoredItem storedItem,
      AppUser claimant,
      LocalDateTime now
  ) {
    StoredItemStatus previousStatus = storedItem.getPublicStatus();
    if (!storedItem.changeStatus(StoredItemStatus.IN_PROGRESS, now)) {
      return;
    }
    itemHistoryRepository.save(ItemStatusHistory.recordTransition(
        storedItem,
        claimant,
        previousStatus,
        StoredItemStatus.IN_PROGRESS,
        "소유자 확인 요청 등록",
        now
    ));
  }

  private void saveAttachments(
      ItemClaim claim,
      AppUser claimant,
      List<MultipartFile> files,
      LocalDateTime now,
      List<StoredFile> storedFiles
  ) {
    List<ItemClaimAttachment> attachments = new ArrayList<>();
    for (MultipartFile file : files) {
      StoredFile storedFile = fileStorageRegistry
          .writeStorage()
          .store(file);
      storedFiles.add(storedFile);
      FileResource fileResource = FileResource.create(
          storedFile.storageProvider(),
          storedFile.storageKey(),
          storedFile.originalFilename(),
          storedFile.mimeType(),
          storedFile.size(),
          storedFile.checksum(),
          claimant,
          now
      );
      fileResourceRepository.save(fileResource);
      attachments.add(ItemClaimAttachment.attach(claim, fileResource));
    }
    if (!attachments.isEmpty()) {
      attachmentRepository.saveAllAndFlush(attachments);
    }
  }

  private void validateFiles(List<MultipartFile> files) {
    if (files.size() > MAX_FILE_COUNT) {
      throw new BusinessException(
          ItemClaimErrorCode.FILE_LIMIT_EXCEEDED
      );
    }
    for (MultipartFile file : files) {
      if (file == null
          || file.isEmpty()
          || !ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
        throw new BusinessException(
            ItemClaimErrorCode.INVALID_FILE_TYPE
        );
      }
    }
  }

  private void deleteStoredFiles(List<StoredFile> storedFiles) {
    for (StoredFile storedFile : storedFiles) {
      try {
        fileStorageRegistry
            .get(storedFile.storageProvider())
            .delete(storedFile.storageKey());
      } catch (FileStorageException ignored) {
        // Preserve the exception that caused the transaction rollback.
      }
    }
  }
}
