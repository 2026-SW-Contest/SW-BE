package org.swbe.domain.lostitem.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.repository.FileResourceRepository;
import org.swbe.domain.lostitem.entity.ItemStatusHistory;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemAttachment;
import org.swbe.domain.lostitem.entity.StoredItemStatus;
import org.swbe.domain.lostitem.exception.StoredItemErrorCode;
import org.swbe.domain.lostitem.repository.ItemClaimRepository;
import org.swbe.domain.lostitem.repository.ItemStatusHistoryRepository;
import org.swbe.domain.lostitem.repository.OfficeStaffAssignmentRepository;
import org.swbe.domain.lostitem.repository.StoredItemAttachmentRepository;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.domain.file.storage.FileStorageRegistry;
import org.swbe.global.error.BusinessException;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoredItemDeleteService {

  private final StoredItemRepository storedItemRepository;
  private final StoredItemAttachmentRepository attachmentRepository;
  private final FileResourceRepository fileResourceRepository;
  private final ItemStatusHistoryRepository statusHistoryRepository;
  private final ItemClaimRepository itemClaimRepository;
  private final OfficeStaffAssignmentRepository assignmentRepository;
  private final FileStorageRegistry fileStorageRegistry;

  @Transactional
  public void delete(
      Long storedItemId,
      Long requesterUserId,
      boolean admin
  ) {
    StoredItem item = storedItemRepository.findDetailById(storedItemId)
        .orElseThrow(() -> new BusinessException(
            StoredItemErrorCode.NOT_FOUND
        ));
    validateOfficeAccess(item, requesterUserId, admin);
    validateDeletable(item);
    validateNoClaims(storedItemId);

    List<StoredItemAttachment> attachments = attachmentRepository
        .findAllByStoredItem_IdOrderByDisplayOrderAscIdAsc(storedItemId);
    List<FileResource> files = attachments.stream()
        .map(StoredItemAttachment::getFile)
        .toList();
    List<StoredObject> storedObjects = files.stream()
        .map(file -> new StoredObject(
            file.getStorageProvider(),
            file.getStorageKey()
        ))
        .toList();
    List<ItemStatusHistory> histories = statusHistoryRepository
        .findAllByStoredItem_IdOrderByIdAsc(storedItemId);

    try {
      deleteDatabaseRows(item, attachments, files, histories);
      scheduleStorageDeletionAfterCommit(storedObjects);
    } catch (OptimisticLockingFailureException exception) {
      throw new BusinessException(StoredItemErrorCode.VERSION_CONFLICT);
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(StoredItemErrorCode.HAS_CLAIMS);
    }
  }

  private void validateOfficeAccess(
      StoredItem item,
      Long userId,
      boolean admin
  ) {
    if (!admin && !assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(
            item.getOffice().getId(),
            userId
        )) {
      throw new BusinessException(StoredItemErrorCode.ACCESS_DENIED);
    }
  }

  private void validateDeletable(StoredItem item) {
    if (item.getPublicStatus() != StoredItemStatus.STORED) {
      throw new BusinessException(StoredItemErrorCode.NOT_DELETABLE);
    }
  }

  private void validateNoClaims(Long storedItemId) {
    if (itemClaimRepository.existsByStoredItem_Id(storedItemId)) {
      throw new BusinessException(StoredItemErrorCode.HAS_CLAIMS);
    }
  }

  private void deleteDatabaseRows(
      StoredItem item,
      List<StoredItemAttachment> attachments,
      List<FileResource> files,
      List<ItemStatusHistory> histories
  ) {
    if (!attachments.isEmpty()) {
      attachmentRepository.deleteAll(attachments);
      attachmentRepository.flush();
    }
    if (!files.isEmpty()) {
      fileResourceRepository.deleteAll(files);
      fileResourceRepository.flush();
    }
    if (!histories.isEmpty()) {
      statusHistoryRepository.deleteAll(histories);
      statusHistoryRepository.flush();
    }
    storedItemRepository.delete(item);
    storedItemRepository.flush();
  }

  private void scheduleStorageDeletionAfterCommit(
      List<StoredObject> storedObjects
  ) {
    if (storedObjects.isEmpty()) {
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            for (StoredObject storedObject : storedObjects) {
              try {
                fileStorageRegistry
                    .get(storedObject.provider())
                    .delete(storedObject.key());
              } catch (RuntimeException exception) {
                log.warn(
                    "Failed to delete stored-item file after item deletion: {}:{}",
                    storedObject.provider(),
                    storedObject.key(),
                    exception
                );
              }
            }
          }
        }
    );
  }

  private record StoredObject(
      String provider,
      String key
  ) {
  }
}
