package org.swbe.domain.facilityrequest.service;

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
import org.swbe.domain.facilityrequest.dto.request.FacilityRequestCreateRequest;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestCreateDataResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestCreateResponse;
import org.swbe.domain.facilityrequest.entity.FacilityCategory;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestAttachment;
import org.swbe.domain.facilityrequest.exception.FacilityRequestErrorCode;
import org.swbe.domain.facilityrequest.repository.FacilityCategoryRepository;
import org.swbe.domain.facilityrequest.repository.FacilityRequestAttachmentRepository;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.repository.FileResourceRepository;
import org.swbe.domain.file.storage.FileStorageException;
import org.swbe.domain.file.storage.FileStorageRegistry;
import org.swbe.domain.file.storage.StoredFile;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.exception.AuthErrorCode;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.global.error.BusinessException;

@Service
@RequiredArgsConstructor
public class FacilityRequestCreateService {

  private static final int MAX_FILE_COUNT = 5;
  private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
      "image/jpeg",
      "image/png",
      "image/gif",
      "image/webp"
  );

  private final FacilityRequestRepository facilityRequestRepository;
  private final FacilityRequestAttachmentRepository attachmentRepository;
  private final FacilityCategoryRepository facilityCategoryRepository;
  private final LocationRepository locationRepository;
  private final AppUserRepository appUserRepository;
  private final FileResourceRepository fileResourceRepository;
  private final FileStorageRegistry fileStorageRegistry;
  private final Clock clock;

  @Transactional
  public FacilityRequestCreateResponse create(
      FacilityRequestCreateRequest request,
      List<MultipartFile> files,
      Long requesterUserId
  ) {
    List<MultipartFile> attachments = files == null ? List.of() : files;
    validateFiles(attachments);

    FacilityCategory category = facilityCategoryRepository
        .findByIdAndActiveTrue(request.categoryId())
        .orElseThrow(() -> new BusinessException(
            FacilityRequestErrorCode.CATEGORY_NOT_FOUND
        ));
    Location location = locationRepository
        .findByIdAndActiveTrueAndBuilding_ActiveTrue(request.locationId())
        .orElseThrow(() -> new BusinessException(
            FacilityRequestErrorCode.LOCATION_NOT_FOUND
        ));
    AppUser requester = appUserRepository.findById(requesterUserId)
        .orElseThrow(() -> new BusinessException(
            AuthErrorCode.ACCOUNT_NOT_FOUND
        ));

    LocalDateTime now = LocalDateTime.now(clock);
    FacilityRequest facilityRequest = FacilityRequest.create(
        category,
        location,
        requester,
        request.title(),
        request.description(),
        now
    );

    List<StoredFile> storedFiles = new ArrayList<>();
    try {
      facilityRequestRepository.save(facilityRequest);
      saveAttachments(
          facilityRequest,
          requester,
          attachments,
          now,
          storedFiles
      );

      return new FacilityRequestCreateResponse(
          new FacilityRequestCreateDataResponse(
              facilityRequest.getId(),
              facilityRequest.getRequestStatus(),
              storedFiles.size(),
              facilityRequest.getCreatedAt()
          )
      );
    } catch (FileStorageException exception) {
      deleteStoredFiles(storedFiles);
      throw new BusinessException(FacilityRequestErrorCode.FILE_STORAGE_ERROR);
    } catch (RuntimeException exception) {
      deleteStoredFiles(storedFiles);
      throw exception;
    }
  }

  private void saveAttachments(
      FacilityRequest facilityRequest,
      AppUser requester,
      List<MultipartFile> files,
      LocalDateTime now,
      List<StoredFile> storedFiles
  ) {
    List<FacilityRequestAttachment> attachments = new ArrayList<>();
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
          requester,
          now
      );
      fileResourceRepository.save(fileResource);
      attachments.add(FacilityRequestAttachment.attach(
          facilityRequest,
          fileResource
      ));
    }
    attachmentRepository.saveAll(attachments);
  }

  private void validateFiles(List<MultipartFile> files) {
    if (files.size() > MAX_FILE_COUNT) {
      throw new BusinessException(
          FacilityRequestErrorCode.FILE_LIMIT_EXCEEDED
      );
    }

    for (MultipartFile file : files) {
      if (file == null
          || file.isEmpty()
          || !ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
        throw new BusinessException(
            FacilityRequestErrorCode.INVALID_FILE_TYPE
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
