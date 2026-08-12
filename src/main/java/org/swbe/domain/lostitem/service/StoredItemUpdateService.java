package org.swbe.domain.lostitem.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.campus.repository.LocationRepository;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.repository.FileResourceRepository;
import org.swbe.domain.file.storage.FileStorageException;
import org.swbe.domain.file.storage.FileStorageRegistry;
import org.swbe.domain.file.storage.StoredFile;
import org.swbe.domain.lostitem.dto.request.StoredItemUpdateRequest;
import org.swbe.domain.lostitem.dto.response.StoredItemUpdateDataResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemUpdateResponse;
import org.swbe.domain.lostitem.entity.ItemCategory;
import org.swbe.domain.lostitem.entity.LostItemOffice;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemAttachment;
import org.swbe.domain.lostitem.exception.StoredItemErrorCode;
import org.swbe.domain.lostitem.repository.ItemCategoryRepository;
import org.swbe.domain.lostitem.repository.LostItemOfficeRepository;
import org.swbe.domain.lostitem.repository.OfficeStaffAssignmentRepository;
import org.swbe.domain.lostitem.repository.StoredItemAttachmentRepository;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.exception.AuthErrorCode;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.global.error.BusinessException;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoredItemUpdateService {

  private static final int MAX_FILE_COUNT = 5;
  private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
      "image/jpeg",
      "image/png",
      "image/gif",
      "image/webp"
  );

  private final StoredItemRepository storedItemRepository;
  private final StoredItemAttachmentRepository attachmentRepository;
  private final LostItemOfficeRepository officeRepository;
  private final OfficeStaffAssignmentRepository assignmentRepository;
  private final ItemCategoryRepository itemCategoryRepository;
  private final LocationRepository locationRepository;
  private final AppUserRepository appUserRepository;
  private final FileResourceRepository fileResourceRepository;
  private final FileStorageRegistry fileStorageRegistry;
  private final Clock clock;

  @Transactional
  public StoredItemUpdateResponse update(
      Long storedItemId,
      StoredItemUpdateRequest request,
      List<MultipartFile> files,
      Long updaterUserId,
      boolean admin
  ) {
    List<MultipartFile> newFiles = files == null ? List.of() : files;
    validateUpdateRequest(request, newFiles);
    validateNewFiles(newFiles);

    StoredItem item = storedItemRepository
        .findDetailById(storedItemId)
        .orElseThrow(() -> new BusinessException(
            StoredItemErrorCode.NOT_FOUND
        ));
    validateOfficeAccess(item.getOffice().getId(), updaterUserId, admin);

    LostItemOffice office = resolveOffice(request, updaterUserId, admin);
    ItemCategory category = resolveCategory(request);
    Location location = resolveLocation(request);
    List<StoredItemAttachment> existingAttachments = attachmentRepository
        .findPublicImagesByStoredItemId(storedItemId);
    List<StoredItemAttachment> retainedAttachments =
        resolveRetainedAttachments(request, existingAttachments);
    Set<Long> retainedFileIds = retainedAttachments.stream()
        .map(attachment -> attachment.getFile().getId())
        .collect(java.util.stream.Collectors.toSet());
    List<StoredItemAttachment> removedAttachments = existingAttachments
        .stream()
        .filter(attachment -> !retainedFileIds.contains(
            attachment.getFile().getId()
        ))
        .toList();
    validateFinalFileCount(retainedAttachments.size(), newFiles.size());

    LocalDateTime now = LocalDateTime.now(clock);
    List<StoredFile> newlyStoredFiles = new ArrayList<>();
    try {
      item.update(
          office,
          category,
          location,
          request == null ? null : request.getFoundLocationText(),
          request != null && request.hasFoundLocationChange(),
          request == null ? null : request.getItemName(),
          request == null ? null : request.getDescription(),
          request == null ? null : request.getPrivateDescription(),
          request != null && request.isPrivateDescriptionProvided(),
          request == null ? null : request.getFoundDate(),
          now
      );
      reorderRetainedAttachments(retainedAttachments);
      List<StoredItemAttachment> addedAttachments = saveNewAttachments(
          item,
          updaterUserId,
          newFiles,
          retainedAttachments.size(),
          now,
          newlyStoredFiles
      );
      List<FileResource> removedFiles = deleteRemovedAttachments(
          removedAttachments
      );
      storedItemRepository.flush();
      scheduleStorageDeletionAfterCommit(removedFiles);

      return new StoredItemUpdateResponse(
          new StoredItemUpdateDataResponse(
              item.getId(),
              item.getPublicStatus().name(),
              retainedAttachments.size() + addedAttachments.size(),
              item.getUpdatedAt()
          )
      );
    } catch (OptimisticLockingFailureException exception) {
      deleteNewlyStoredFiles(newlyStoredFiles);
      throw new BusinessException(StoredItemErrorCode.VERSION_CONFLICT);
    } catch (FileStorageException exception) {
      deleteNewlyStoredFiles(newlyStoredFiles);
      throw new BusinessException(StoredItemErrorCode.FILE_STORAGE_ERROR);
    } catch (RuntimeException exception) {
      deleteNewlyStoredFiles(newlyStoredFiles);
      throw exception;
    }
  }

  private void validateUpdateRequest(
      StoredItemUpdateRequest request,
      List<MultipartFile> files
  ) {
    if ((request == null || !request.hasChanges()) && files.isEmpty()) {
      throw new BusinessException(StoredItemErrorCode.INVALID_REQUEST);
    }
    if (request != null
        && request.hasFoundLocationChange()
        && !request.hasValidFoundLocationChange()) {
      throw new BusinessException(
          StoredItemErrorCode.INVALID_FOUND_LOCATION
      );
    }
  }

  private void validateOfficeAccess(
      Long officeId,
      Long userId,
      boolean admin
  ) {
    if (!admin && !assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(officeId, userId)) {
      throw new BusinessException(StoredItemErrorCode.ACCESS_DENIED);
    }
  }

  private LostItemOffice resolveOffice(
      StoredItemUpdateRequest request,
      Long userId,
      boolean admin
  ) {
    if (request == null || request.getOfficeId() == null) {
      return null;
    }
    LostItemOffice office = officeRepository
        .findByIdAndActiveTrue(request.getOfficeId())
        .orElseThrow(() -> new BusinessException(
            StoredItemErrorCode.OFFICE_NOT_FOUND
        ));
    validateOfficeAccess(office.getId(), userId, admin);
    return office;
  }

  private ItemCategory resolveCategory(StoredItemUpdateRequest request) {
    if (request == null || request.getCategoryId() == null) {
      return null;
    }
    return itemCategoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new BusinessException(
            StoredItemErrorCode.CATEGORY_NOT_FOUND
        ));
  }

  private Location resolveLocation(StoredItemUpdateRequest request) {
    if (request == null || request.getFoundLocationId() == null) {
      return null;
    }
    return locationRepository
        .findByIdAndActiveTrueAndBuilding_ActiveTrue(
            request.getFoundLocationId()
        )
        .orElseThrow(() -> new BusinessException(
            StoredItemErrorCode.LOCATION_NOT_FOUND
        ));
  }

  private List<StoredItemAttachment> resolveRetainedAttachments(
      StoredItemUpdateRequest request,
      List<StoredItemAttachment> existingAttachments
  ) {
    if (request == null || request.getKeepFileIds() == null) {
      return List.copyOf(existingAttachments);
    }

    Map<Long, StoredItemAttachment> attachmentsByFileId =
        new LinkedHashMap<>();
    for (StoredItemAttachment attachment : existingAttachments) {
      attachmentsByFileId.put(attachment.getFile().getId(), attachment);
    }
    List<Long> keepFileIds = request.getKeepFileIds();
    if (new HashSet<>(keepFileIds).size() != keepFileIds.size()
        || !attachmentsByFileId.keySet().containsAll(keepFileIds)) {
      throw new BusinessException(StoredItemErrorCode.INVALID_ATTACHMENT);
    }
    return keepFileIds.stream()
        .map(attachmentsByFileId::get)
        .toList();
  }

  private void reorderRetainedAttachments(
      List<StoredItemAttachment> attachments
  ) {
    for (int index = 0; index < attachments.size(); index++) {
      attachments.get(index).reorder(index == 0, index);
    }
  }

  private List<StoredItemAttachment> saveNewAttachments(
      StoredItem item,
      Long updaterUserId,
      List<MultipartFile> files,
      int startOrder,
      LocalDateTime now,
      List<StoredFile> newlyStoredFiles
  ) {
    if (files.isEmpty()) {
      return List.of();
    }
    AppUser updater = appUserRepository.findById(updaterUserId)
        .orElseThrow(() -> new BusinessException(
            AuthErrorCode.ACCOUNT_NOT_FOUND
        ));
    List<StoredItemAttachment> attachments = new ArrayList<>();
    for (int index = 0; index < files.size(); index++) {
      StoredFile storedFile = fileStorageRegistry
          .writeStorage()
          .store(files.get(index));
      newlyStoredFiles.add(storedFile);
      FileResource fileResource = FileResource.create(
          storedFile.storageProvider(),
          storedFile.storageKey(),
          storedFile.originalFilename(),
          storedFile.mimeType(),
          storedFile.size(),
          storedFile.checksum(),
          updater,
          now
      );
      fileResourceRepository.save(fileResource);
      int displayOrder = startOrder + index;
      attachments.add(StoredItemAttachment.attach(
          item,
          fileResource,
          displayOrder == 0,
          displayOrder
      ));
    }
    return attachmentRepository.saveAll(attachments);
  }

  private List<FileResource> deleteRemovedAttachments(
      List<StoredItemAttachment> removedAttachments
  ) {
    if (removedAttachments.isEmpty()) {
      return List.of();
    }
    List<FileResource> files = removedAttachments.stream()
        .map(StoredItemAttachment::getFile)
        .toList();
    attachmentRepository.deleteAll(removedAttachments);
    attachmentRepository.flush();
    fileResourceRepository.deleteAll(files);
    fileResourceRepository.flush();
    return files;
  }

  private void scheduleStorageDeletionAfterCommit(
      List<FileResource> files
  ) {
    if (files.isEmpty()) {
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            for (FileResource file : files) {
              try {
                fileStorageRegistry
                    .get(file.getStorageProvider())
                    .delete(file.getStorageKey());
              } catch (RuntimeException exception) {
                log.warn(
                    "Failed to delete detached stored-item file: {}:{}",
                    file.getStorageProvider(),
                    file.getStorageKey(),
                    exception
                );
              }
            }
          }
        }
    );
  }

  private void validateNewFiles(List<MultipartFile> files) {
    for (MultipartFile file : files) {
      if (file == null
          || file.isEmpty()
          || !ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
        throw new BusinessException(
            StoredItemErrorCode.INVALID_FILE_TYPE
        );
      }
    }
  }

  private void validateFinalFileCount(
      int retainedFileCount,
      int newFileCount
  ) {
    if (retainedFileCount + newFileCount > MAX_FILE_COUNT) {
      throw new BusinessException(
          StoredItemErrorCode.FILE_LIMIT_EXCEEDED
      );
    }
  }

  private void deleteNewlyStoredFiles(List<StoredFile> storedFiles) {
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
