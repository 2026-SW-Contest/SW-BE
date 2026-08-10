package org.swbe.domain.facilityrequest.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestAttachment;
import org.swbe.domain.facilityrequest.exception.FacilityRequestErrorCode;
import org.swbe.domain.facilityrequest.repository.FacilityRequestAttachmentRepository;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.repository.FileResourceRepository;
import org.swbe.domain.file.storage.FileStorage;
import org.swbe.domain.file.storage.FileStorageException;
import org.swbe.global.error.BusinessException;

class FacilityRequestDeleteServiceTest {

  private FacilityRequestRepository facilityRequestRepository;
  private FacilityRequestAttachmentRepository attachmentRepository;
  private FileResourceRepository fileResourceRepository;
  private FileStorage fileStorage;
  private FacilityRequestDeleteService service;

  @BeforeEach
  void setUp() {
    facilityRequestRepository = mock(FacilityRequestRepository.class);
    attachmentRepository = mock(
        FacilityRequestAttachmentRepository.class
    );
    fileResourceRepository = mock(FileResourceRepository.class);
    fileStorage = mock(FileStorage.class);
    service = new FacilityRequestDeleteService(
        facilityRequestRepository,
        attachmentRepository,
        fileResourceRepository,
        fileStorage
    );
  }

  @Test
  void authorCanDeleteReceivedFacilityRequestWithAttachments() {
    FacilityRequest request = deletableRequest(true, true);
    FacilityRequestAttachment attachment =
        mock(FacilityRequestAttachment.class);
    FileResource file = mock(FileResource.class);
    when(file.getStorageKey()).thenReturn("2026/08/09/image.jpg");
    when(attachment.getFile()).thenReturn(file);
    when(facilityRequestRepository.findDetailById(25L))
        .thenReturn(Optional.of(request));
    when(attachmentRepository
        .findAllByFacilityRequest_IdOrderByIdAsc(25L))
        .thenReturn(List.of(attachment));

    service.delete(25L, 7L);

    verify(fileStorage).delete("2026/08/09/image.jpg");
    verify(attachmentRepository).deleteAllInBatch(List.of(attachment));
    verify(fileResourceRepository).deleteAllInBatch(List.of(file));
    verify(facilityRequestRepository).delete(request);
  }

  @Test
  void missingFacilityRequestCannotBeDeleted() {
    when(facilityRequestRepository.findDetailById(25L))
        .thenReturn(Optional.empty());

    assertBusinessError(
        () -> service.delete(25L, 7L),
        FacilityRequestErrorCode.NOT_FOUND
    );
  }

  @Test
  void userWhoIsNotAuthorCannotDeleteFacilityRequest() {
    FacilityRequest request = deletableRequest(false, true);
    when(facilityRequestRepository.findDetailById(25L))
        .thenReturn(Optional.of(request));

    assertBusinessError(
        () -> service.delete(25L, 8L),
        FacilityRequestErrorCode.ACCESS_DENIED
    );
    verify(facilityRequestRepository, never()).delete(request);
  }

  @Test
  void nonReceivedFacilityRequestCannotBeDeleted() {
    FacilityRequest request = deletableRequest(true, false);
    when(facilityRequestRepository.findDetailById(25L))
        .thenReturn(Optional.of(request));

    assertBusinessError(
        () -> service.delete(25L, 7L),
        FacilityRequestErrorCode.NOT_DELETABLE
    );
    verify(facilityRequestRepository, never()).delete(request);
  }

  @Test
  void storageFailureStopsFacilityRequestDeletion() {
    FacilityRequest request = deletableRequest(true, true);
    FacilityRequestAttachment attachment =
        mock(FacilityRequestAttachment.class);
    FileResource file = mock(FileResource.class);
    when(file.getStorageKey()).thenReturn("2026/08/09/image.jpg");
    when(attachment.getFile()).thenReturn(file);
    when(facilityRequestRepository.findDetailById(25L))
        .thenReturn(Optional.of(request));
    when(attachmentRepository
        .findAllByFacilityRequest_IdOrderByIdAsc(25L))
        .thenReturn(List.of(attachment));
    org.mockito.Mockito.doThrow(new FileStorageException(
            "failed",
            new RuntimeException("storage unavailable")
        ))
        .when(fileStorage)
        .delete("2026/08/09/image.jpg");

    assertBusinessError(
        () -> service.delete(25L, 7L),
        FacilityRequestErrorCode.FILE_STORAGE_ERROR
    );
    verify(facilityRequestRepository, never()).delete(request);
  }

  private FacilityRequest deletableRequest(
      boolean author,
      boolean deletable
  ) {
    FacilityRequest request = mock(FacilityRequest.class);
    when(request.isRequestedBy(7L)).thenReturn(author);
    when(request.isRequestedBy(8L)).thenReturn(author);
    when(request.isDeletable()).thenReturn(deletable);
    return request;
  }

  private void assertBusinessError(
      Runnable action,
      FacilityRequestErrorCode expectedError
  ) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessException.class)
        .extracting(exception ->
            ((BusinessException) exception).getErrorCode()
        )
        .isEqualTo(expectedError);
  }
}
