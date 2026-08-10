package org.swbe.domain.facilityrequest.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.campus.repository.LocationRepository;
import org.swbe.domain.facilityrequest.dto.request.FacilityRequestUpdateRequest;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestUpdateDataResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestUpdateResponse;
import org.swbe.domain.facilityrequest.entity.FacilityCategory;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestAttachment;
import org.swbe.domain.facilityrequest.exception.FacilityRequestErrorCode;
import org.swbe.domain.facilityrequest.repository.FacilityCategoryRepository;
import org.swbe.domain.facilityrequest.repository.FacilityRequestAttachmentRepository;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.repository.FileResourceRepository;
import org.swbe.domain.file.storage.FileStorage;
import org.swbe.domain.file.storage.FileStorageException;
import org.swbe.domain.file.storage.StoredFile;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.global.error.BusinessException;

@Service
public class FacilityRequestUpdateService {

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
  private final FileResourceRepository fileResourceRepository;
  private final FileStorage fileStorage;
  private final Clock clock;

  // 문의 수정에 필요한 JPA 저장소와 실제 파일 저장소를 주입받는다.
  public FacilityRequestUpdateService(
      FacilityRequestRepository facilityRequestRepository,
      FacilityRequestAttachmentRepository attachmentRepository,
      FacilityCategoryRepository facilityCategoryRepository,
      LocationRepository locationRepository,
      FileResourceRepository fileResourceRepository,
      FileStorage fileStorage,
      Clock clock
  ) {
    this.facilityRequestRepository = facilityRequestRepository;
    this.attachmentRepository = attachmentRepository;
    this.facilityCategoryRepository = facilityCategoryRepository;
    this.locationRepository = locationRepository;
    this.fileResourceRepository = fileResourceRepository;
    this.fileStorage = fileStorage;
    this.clock = clock;
  }

  // 작성자와 상태를 검증하고 문의 정보와 첨부파일을 함께 수정한다.
  @Transactional
  public FacilityRequestUpdateResponse update(
      Long facilityRequestId,
      FacilityRequestUpdateRequest request,
      List<MultipartFile> files,
      Long requesterUserId
  ) {
    List<MultipartFile> newFiles = files == null ? List.of() : files;
    validateUpdateRequest(request, newFiles);
    validateNewFiles(newFiles);

    FacilityRequest facilityRequest = facilityRequestRepository
        .findDetailById(facilityRequestId)
        .orElseThrow(() -> new BusinessException(
            FacilityRequestErrorCode.NOT_FOUND
        ));
    validateAuthor(facilityRequest, requesterUserId);
    validateEditable(facilityRequest);

    FacilityCategory category = findCategory(request);
    Location location = findLocation(request);
    List<FacilityRequestAttachment> existingAttachments =
        attachmentRepository
            .findAllByFacilityRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(
                facilityRequestId
            );
    Set<Long> keepFileIds = resolveKeepFileIds(
        request,
        existingAttachments
    );
    List<FacilityRequestAttachment> retainedAttachments = existingAttachments
        .stream()
        .filter(attachment -> keepFileIds.contains(
            attachment.getFile().getId()
        ))
        .toList();
    List<FacilityRequestAttachment> removedAttachments = existingAttachments
        .stream()
        .filter(attachment -> !keepFileIds.contains(
            attachment.getFile().getId()
        ))
        .toList();
    validateFinalFileCount(retainedAttachments.size(), newFiles.size());

    LocalDateTime now = LocalDateTime.now(clock);
    List<StoredFile> newlyStoredFiles = new ArrayList<>();
    try {
      facilityRequest.update(
          category,
          location,
          request == null ? null : request.title(),
          request == null ? null : request.description(),
          now
      );
      saveNewAttachments(
          facilityRequest,
          facilityRequest.getRequester(),
          newFiles,
          now,
          newlyStoredFiles
      );
      deleteRemovedAttachments(removedAttachments);

      return new FacilityRequestUpdateResponse(
          new FacilityRequestUpdateDataResponse(
              facilityRequest.getId(),
              facilityRequest.getRequestStatus(),
              retainedAttachments.size() + newlyStoredFiles.size(),
              facilityRequest.getUpdatedAt()
          )
      );
    } catch (FileStorageException exception) {
      deleteNewlyStoredFiles(newlyStoredFiles);
      throw new BusinessException(
          FacilityRequestErrorCode.FILE_STORAGE_ERROR
      );
    } catch (RuntimeException exception) {
      deleteNewlyStoredFiles(newlyStoredFiles);
      throw exception;
    }
  }

  // 문의 정보 또는 신규 첨부파일 중 하나 이상의 변경값이 있는지 확인한다.
  private void validateUpdateRequest(
      FacilityRequestUpdateRequest request,
      List<MultipartFile> files
  ) {
    boolean hasRequestChanges = request != null && request.hasChanges();
    if (!hasRequestChanges && files.isEmpty()) {
      throw new BusinessException(
          FacilityRequestErrorCode.INVALID_REQUEST
      );
    }
  }

  // 현재 사용자가 문의 작성자인지 엔티티를 통해 확인한다.
  private void validateAuthor(
      FacilityRequest facilityRequest,
      Long requesterUserId
  ) {
    if (!facilityRequest.isRequestedBy(requesterUserId)) {
      throw new BusinessException(
          FacilityRequestErrorCode.ACCESS_DENIED
      );
    }
  }

  // 문의가 접수 상태여서 수정 가능한지 엔티티를 통해 확인한다.
  private void validateEditable(FacilityRequest facilityRequest) {
    if (!facilityRequest.isEditable()) {
      throw new BusinessException(
          FacilityRequestErrorCode.NOT_EDITABLE
      );
    }
  }

  // 카테고리 변경 요청이 있으면 활성 카테고리를 조회한다.
  private FacilityCategory findCategory(
      FacilityRequestUpdateRequest request
  ) {
    if (request == null || request.categoryId() == null) {
      return null;
    }
    return facilityCategoryRepository
        .findByIdAndActiveTrue(request.categoryId())
        .orElseThrow(() -> new BusinessException(
            FacilityRequestErrorCode.CATEGORY_NOT_FOUND
        ));
  }

  // 장소 변경 요청이 있으면 활성 장소를 조회한다.
  private Location findLocation(FacilityRequestUpdateRequest request) {
    if (request == null || request.locationId() == null) {
      return null;
    }
    return locationRepository
        .findByIdAndActiveTrueAndBuilding_ActiveTrue(request.locationId())
        .orElseThrow(() -> new BusinessException(
            FacilityRequestErrorCode.LOCATION_NOT_FOUND
        ));
  }

  // 생략 시 기존 파일을 모두 유지하고, 전달 시 해당 문의의 파일인지 확인한다.
  private Set<Long> resolveKeepFileIds(
      FacilityRequestUpdateRequest request,
      List<FacilityRequestAttachment> existingAttachments
  ) {
    Set<Long> existingFileIds = new HashSet<>();
    for (FacilityRequestAttachment attachment : existingAttachments) {
      existingFileIds.add(attachment.getFile().getId());
    }

    if (request == null || request.keepFileIds() == null) {
      return existingFileIds;
    }

    Set<Long> requestedFileIds = new HashSet<>(request.keepFileIds());
    if (!existingFileIds.containsAll(requestedFileIds)) {
      throw new BusinessException(
          FacilityRequestErrorCode.INVALID_ATTACHMENT
      );
    }
    return requestedFileIds;
  }

  // 신규 파일이 비어 있지 않은 허용 이미지 형식인지 확인한다.
  private void validateNewFiles(List<MultipartFile> files) {
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

  // 유지할 파일과 신규 파일의 합이 최대 첨부 개수를 넘지 않는지 확인한다.
  private void validateFinalFileCount(
      int retainedFileCount,
      int newFileCount
  ) {
    if (retainedFileCount + newFileCount > MAX_FILE_COUNT) {
      throw new BusinessException(
          FacilityRequestErrorCode.FILE_LIMIT_EXCEEDED
      );
    }
  }

  // 신규 파일을 실제 저장소와 파일 및 첨부 관계 테이블에 저장한다.
  private void saveNewAttachments(
      FacilityRequest facilityRequest,
      AppUser requester,
      List<MultipartFile> files,
      LocalDateTime now,
      List<StoredFile> newlyStoredFiles
  ) {
    List<FacilityRequestAttachment> newAttachments = new ArrayList<>();
    for (MultipartFile file : files) {
      StoredFile storedFile = fileStorage.store(file);
      newlyStoredFiles.add(storedFile);

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
      newAttachments.add(FacilityRequestAttachment.attach(
          facilityRequest,
          fileResource
      ));
    }
    attachmentRepository.saveAll(newAttachments);
  }

  // 유지 대상에서 제외된 기존 첨부 관계, 파일 정보와 실제 파일을 삭제한다.
  private void deleteRemovedAttachments(
      List<FacilityRequestAttachment> removedAttachments
  ) {
    if (removedAttachments.isEmpty()) {
      return;
    }

    List<FileResource> removedFiles = removedAttachments.stream()
        .map(FacilityRequestAttachment::getFile)
        .toList();
    for (FileResource file : removedFiles) {
      fileStorage.delete(file.getStorageKey());
    }
    attachmentRepository.deleteAllInBatch(removedAttachments);
    fileResourceRepository.deleteAllInBatch(removedFiles);
  }

  // 수정 실패 시 이번 요청에서 새로 저장한 실제 파일을 조용히 정리한다.
  private void deleteNewlyStoredFiles(List<StoredFile> newlyStoredFiles) {
    for (StoredFile storedFile : newlyStoredFiles) {
      try {
        fileStorage.delete(storedFile.storageKey());
      } catch (FileStorageException ignored) {
        // 원래 발생한 수정 실패 예외를 유지한다.
      }
    }
  }
}
