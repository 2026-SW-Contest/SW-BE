package org.swbe.domain.facilityrequest.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestAttachment;
import org.swbe.domain.facilityrequest.exception.FacilityRequestErrorCode;
import org.swbe.domain.facilityrequest.repository.FacilityRequestAttachmentRepository;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.repository.FileResourceRepository;
import org.swbe.domain.file.storage.FileStorage;
import org.swbe.domain.file.storage.FileStorageException;
import org.swbe.domain.file.storage.FileStorageRegistry;
import org.swbe.global.error.BusinessException;

@Service
public class FacilityRequestDeleteService {

  private final FacilityRequestRepository facilityRequestRepository;
  private final FacilityRequestAttachmentRepository attachmentRepository;
  private final FileResourceRepository fileResourceRepository;
  private final FileStorageRegistry fileStorageRegistry;

  // 문의 삭제에 필요한 JPA 저장소와 실제 파일 저장소를 주입받는다.
  public FacilityRequestDeleteService(
      FacilityRequestRepository facilityRequestRepository,
      FacilityRequestAttachmentRepository attachmentRepository,
      FileResourceRepository fileResourceRepository,
      FileStorageRegistry fileStorageRegistry
  ) {
    this.facilityRequestRepository = facilityRequestRepository;
    this.attachmentRepository = attachmentRepository;
    this.fileResourceRepository = fileResourceRepository;
    this.fileStorageRegistry = fileStorageRegistry;
  }

  // 문의 존재 여부, 작성자, 삭제 가능 상태를 검증한 뒤 문의와 첨부파일을 삭제한다.
  @Transactional
  public void delete(Long facilityRequestId, Long requesterUserId) {
    FacilityRequest facilityRequest = facilityRequestRepository
        .findDetailById(facilityRequestId)
        .orElseThrow(() -> new BusinessException(
            FacilityRequestErrorCode.NOT_FOUND
        ));

    validateAuthor(facilityRequest, requesterUserId);
    validateDeletable(facilityRequest);

    List<FacilityRequestAttachment> attachments = attachmentRepository
        .findAllByFacilityRequest_IdOrderByIdAsc(facilityRequestId);
    List<FileResource> files = attachments.stream()
        .map(FacilityRequestAttachment::getFile)
        .toList();

    deleteStoredFiles(files);

    attachmentRepository.deleteAll(attachments);
    attachmentRepository.flush();

    fileResourceRepository.deleteAll(files);
    fileResourceRepository.flush();

    facilityRequestRepository.delete(facilityRequest);
  }

  // 현재 사용자가 문의 작성자인지 확인한다.
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

  // 문의 상태가 삭제 가능한 접수 상태인지 확인한다.
  private void validateDeletable(FacilityRequest facilityRequest) {
    if (!facilityRequest.isDeletable()) {
      throw new BusinessException(
          FacilityRequestErrorCode.NOT_DELETABLE
      );
    }
  }

  // DB에서 첨부 정보를 지우기 전에 실제 저장소의 파일을 삭제한다.
  private void deleteStoredFiles(List<FileResource> files) {
    try {
      for (FileResource file : files) {
        FileStorage fileStorage = fileStorageRegistry.get(
            file.getStorageProvider()
        );
        fileStorage.delete(file.getStorageKey());
      }
    } catch (FileStorageException exception) {
      throw new BusinessException(
          FacilityRequestErrorCode.FILE_STORAGE_ERROR
      );
    }
  }
}
