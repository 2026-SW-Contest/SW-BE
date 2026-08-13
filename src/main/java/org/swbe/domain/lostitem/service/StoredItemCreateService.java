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
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.campus.repository.LocationRepository;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.repository.FileResourceRepository;
import org.swbe.domain.file.storage.FileStorageException;
import org.swbe.domain.file.storage.FileStorageRegistry;
import org.swbe.domain.file.storage.StoredFile;
import org.swbe.domain.lostitem.dto.request.StoredItemCreateRequest;
import org.swbe.domain.lostitem.dto.response.StoredItemCreateDataResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemCreateResponse;
import org.swbe.domain.lostitem.entity.ItemCategory;
import org.swbe.domain.lostitem.entity.ItemStatusHistory;
import org.swbe.domain.lostitem.entity.LostItemOffice;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemAttachment;
import org.swbe.domain.lostitem.exception.StoredItemErrorCode;
import org.swbe.domain.lostitem.repository.ItemCategoryRepository;
import org.swbe.domain.lostitem.repository.ItemStatusHistoryRepository;
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
public class StoredItemCreateService {

  private static final int MAX_FILE_COUNT = 5;
  private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
      "image/jpeg",
      "image/png",
      "image/gif",
      "image/webp"
  );

  private final StoredItemRepository storedItemRepository;
  private final StoredItemAttachmentRepository attachmentRepository;
  private final ItemStatusHistoryRepository statusHistoryRepository;
  private final LostItemOfficeRepository officeRepository;
  private final OfficeStaffAssignmentRepository assignmentRepository;
  private final ItemCategoryRepository itemCategoryRepository;
  private final LocationRepository locationRepository;
  private final AppUserRepository appUserRepository;
  private final FileResourceRepository fileResourceRepository;
  private final FileStorageRegistry fileStorageRegistry;
  private final Clock clock;

  @Transactional
  public StoredItemCreateResponse create(
      StoredItemCreateRequest request,
      List<MultipartFile> files,
      Long registrantUserId,
      boolean admin
  ) {
    List<MultipartFile> images = files == null ? List.of() : files;
    validateRequest(request);
    validateFiles(images);

    LostItemOffice office = officeRepository
        .findByIdAndActiveTrue(request.officeId())
        .orElseThrow(() -> new BusinessException(
            StoredItemErrorCode.OFFICE_NOT_FOUND
        ));
    if (!admin && !assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(
            office.getId(),
            registrantUserId
        )) {
      throw new BusinessException(StoredItemErrorCode.ACCESS_DENIED);
    }
    ItemCategory category = itemCategoryRepository
        .findById(request.categoryId())
        .orElseThrow(() -> new BusinessException(
            StoredItemErrorCode.CATEGORY_NOT_FOUND
        ));
    Location foundLocation = resolveFoundLocation(request);
    AppUser registrant = appUserRepository.findById(registrantUserId)
        .orElseThrow(() -> new BusinessException(
            AuthErrorCode.ACCOUNT_NOT_FOUND
        ));
    LocalDateTime now = LocalDateTime.now(clock);
    StoredItem storedItem = StoredItem.create(
        office,
        foundLocation,
        request.foundLocationText(),
        registrant,
        category,
        request.itemName(),
        request.description(),
        request.privateDescription(),
        request.foundDate(),
        now
    );

    List<StoredFile> storedFiles = new ArrayList<>();
    try {
      storedItemRepository.save(storedItem);
      statusHistoryRepository.save(ItemStatusHistory.recordInitial(
          storedItem,
          registrant,
          now
      ));
      saveAttachments(
          storedItem,
          registrant,
          images,
          now,
          storedFiles
      );
      storedItemRepository.flush();

      return new StoredItemCreateResponse(
          new StoredItemCreateDataResponse(
              storedItem.getId(),
              storedItem.getPublicStatus().name(),
              storedFiles.size(),
              storedItem.getCreatedAt()
          )
      );
    } catch (FileStorageException exception) {
      deleteStoredFiles(storedFiles);
      throw new BusinessException(StoredItemErrorCode.FILE_STORAGE_ERROR);
    } catch (RuntimeException exception) {
      deleteStoredFiles(storedFiles);
      throw exception;
    }
  }

  private Location resolveFoundLocation(StoredItemCreateRequest request) {
    if (request.foundLocationId() == null) {
      return null;
    }
    return locationRepository
        .findByIdAndActiveTrueAndBuilding_ActiveTrue(
            request.foundLocationId()
        )
        .orElseThrow(() -> new BusinessException(
            StoredItemErrorCode.LOCATION_NOT_FOUND
        ));
  }

  private void saveAttachments(
      StoredItem storedItem,
      AppUser registrant,
      List<MultipartFile> files,
      LocalDateTime now,
      List<StoredFile> storedFiles
  ) {
    List<StoredItemAttachment> attachments = new ArrayList<>();
    for (int index = 0; index < files.size(); index++) {
      StoredFile storedFile = fileStorageRegistry
          .writeStorage()
          .store(files.get(index));
      storedFiles.add(storedFile);
      FileResource fileResource = FileResource.create(
          storedFile.storageProvider(),
          storedFile.storageKey(),
          storedFile.originalFilename(),
          storedFile.mimeType(),
          storedFile.size(),
          storedFile.checksum(),
          registrant,
          now
      );
      fileResourceRepository.save(fileResource);
      attachments.add(StoredItemAttachment.attach(
          storedItem,
          fileResource,
          index == 0,
          index
      ));
    }
    attachmentRepository.saveAllAndFlush(attachments);
  }

  private void validateRequest(StoredItemCreateRequest request) {
    if (!request.hasRequiredFoundLocation()) {
      throw new BusinessException(
          StoredItemErrorCode.INVALID_FOUND_LOCATION
      );
    }
  }

  private void validateFiles(List<MultipartFile> files) {
    if (files.size() > MAX_FILE_COUNT) {
      throw new BusinessException(
          StoredItemErrorCode.FILE_LIMIT_EXCEEDED
      );
    }
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
