package org.swbe.domain.facilityrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.campus.repository.LocationRepository;
import org.swbe.domain.facilityrequest.dto.request.FacilityRequestUpdateRequest;
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

class FacilityRequestUpdateServiceTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(
      Instant.parse("2026-08-09T07:30:00Z"),
      ZoneOffset.UTC
  );

  private FacilityRequestRepository facilityRequestRepository;
  private FacilityRequestAttachmentRepository attachmentRepository;
  private FacilityCategoryRepository facilityCategoryRepository;
  private LocationRepository locationRepository;
  private FileResourceRepository fileResourceRepository;
  private FileStorage fileStorage;
  private FacilityRequestUpdateService updateService;

  @BeforeEach
  void setUp() {
    facilityRequestRepository = mock(FacilityRequestRepository.class);
    attachmentRepository = mock(
        FacilityRequestAttachmentRepository.class
    );
    facilityCategoryRepository = mock(FacilityCategoryRepository.class);
    locationRepository = mock(LocationRepository.class);
    fileResourceRepository = mock(FileResourceRepository.class);
    fileStorage = mock(FileStorage.class);
    updateService = new FacilityRequestUpdateService(
        facilityRequestRepository,
        attachmentRepository,
        facilityCategoryRepository,
        locationRepository,
        fileResourceRepository,
        fileStorage,
        FIXED_CLOCK
    );
  }

  @Test
  void updatesTitleAndAddsImageWhileKeepingExistingImage() {
    FacilityRequest facilityRequest = receivedRequest(7L);
    FacilityRequestAttachment existingAttachment = attachment(
        15L,
        "existing.jpg"
    );
    MockMultipartFile newImage = image("new.jpg", "image/jpeg");
    StoredFile storedFile = storedFile("new.jpg");
    prepareRequest(facilityRequest, List.of(existingAttachment));
    when(fileStorage.store(newImage)).thenReturn(storedFile);

    FacilityRequestUpdateResponse response = updateService.update(
        25L,
        new FacilityRequestUpdateRequest(
            null,
            null,
            "Updated title",
            null,
            List.of(15L)
        ),
        List.of(newImage),
        7L
    );

    assertThat(facilityRequest.getTitle()).isEqualTo("Updated title");
    assertThat(facilityRequest.getDescription())
        .isEqualTo("Original description");
    assertThat(response.data().attachmentCount()).isEqualTo(2);
    assertThat(response.data().updatedAt()).isEqualTo(
        LocalDateTime.of(2026, 8, 9, 7, 30)
    );
    verify(fileStorage).store(newImage);
    verify(fileResourceRepository).save(any(FileResource.class));
    verify(attachmentRepository).saveAll(any());
    verify(attachmentRepository, never()).deleteAllInBatch(any());
  }

  @Test
  void updatesCategoryLocationAndDescription() {
    FacilityRequest facilityRequest = receivedRequest(7L);
    FacilityCategory updatedCategory = mock(FacilityCategory.class);
    Location updatedLocation = mock(Location.class);
    prepareRequest(facilityRequest, List.of());
    when(facilityCategoryRepository.findByIdAndActiveTrue(2L))
        .thenReturn(Optional.of(updatedCategory));
    when(locationRepository
        .findByIdAndActiveTrueAndBuilding_ActiveTrue(3L))
        .thenReturn(Optional.of(updatedLocation));

    updateService.update(
        25L,
        new FacilityRequestUpdateRequest(
            2L,
            3L,
            null,
            "Updated description",
            null
        ),
        List.of(),
        7L
    );

    assertThat(facilityRequest.getFacilityCategory())
        .isSameAs(updatedCategory);
    assertThat(facilityRequest.getLocation()).isSameAs(updatedLocation);
    assertThat(facilityRequest.getDescription())
        .isEqualTo("Updated description");
  }

  @Test
  void emptyUpdateIsRejected() {
    assertBusinessError(
        () -> updateService.update(25L, null, List.of(), 7L),
        FacilityRequestErrorCode.INVALID_REQUEST
    );

    verifyNoInteractions(facilityRequestRepository);
  }

  @Test
  void nonAuthorCannotUpdateFacilityRequest() {
    FacilityRequest facilityRequest = receivedRequest(8L);
    when(facilityRequestRepository.findDetailById(25L))
        .thenReturn(Optional.of(facilityRequest));

    assertBusinessError(
        () -> updateService.update(
            25L,
            titleUpdate(),
            List.of(),
            7L
        ),
        FacilityRequestErrorCode.ACCESS_DENIED
    );
    verifyNoInteractions(attachmentRepository);
  }

  @Test
  void nonReceivedFacilityRequestCannotBeUpdated() {
    FacilityRequest facilityRequest = receivedRequest(7L);
    ReflectionTestUtils.setField(
        facilityRequest,
        "requestStatus",
        "IN_PROGRESS"
    );
    when(facilityRequestRepository.findDetailById(25L))
        .thenReturn(Optional.of(facilityRequest));

    assertBusinessError(
        () -> updateService.update(
            25L,
            titleUpdate(),
            List.of(),
            7L
        ),
        FacilityRequestErrorCode.NOT_EDITABLE
    );
  }

  @Test
  void attachmentFromAnotherRequestIsRejected() {
    FacilityRequest facilityRequest = receivedRequest(7L);
    prepareRequest(
        facilityRequest,
        List.of(attachment(15L, "existing.jpg"))
    );

    assertBusinessError(
        () -> updateService.update(
            25L,
            new FacilityRequestUpdateRequest(
                null,
                null,
                null,
                null,
                List.of(99L)
            ),
            List.of(),
            7L
        ),
        FacilityRequestErrorCode.INVALID_ATTACHMENT
    );
  }

  @Test
  void finalAttachmentCountCannotExceedFive() {
    FacilityRequest facilityRequest = receivedRequest(7L);
    List<FacilityRequestAttachment> attachments = List.of(
        attachment(1L, "1.jpg"),
        attachment(2L, "2.jpg"),
        attachment(3L, "3.jpg"),
        attachment(4L, "4.jpg"),
        attachment(5L, "5.jpg")
    );
    prepareRequest(facilityRequest, attachments);

    assertBusinessError(
        () -> updateService.update(
            25L,
            titleUpdate(),
            List.of(image("new.jpg", "image/jpeg")),
            7L
        ),
        FacilityRequestErrorCode.FILE_LIMIT_EXCEEDED
    );
    verify(fileStorage, never()).store(any());
  }

  @Test
  void emptyKeepFileIdsDeletesAllExistingImages() {
    FacilityRequest facilityRequest = receivedRequest(7L);
    FacilityRequestAttachment attachment = attachment(
        15L,
        "existing.jpg"
    );
    prepareRequest(facilityRequest, List.of(attachment));

    FacilityRequestUpdateResponse response = updateService.update(
        25L,
        new FacilityRequestUpdateRequest(
            null,
            null,
            null,
            null,
            List.of()
        ),
        List.of(),
        7L
    );

    assertThat(response.data().attachmentCount()).isZero();
    verify(fileStorage).delete("existing.jpg");
    verify(attachmentRepository).deleteAllInBatch(List.of(attachment));
    verify(fileResourceRepository).deleteAllInBatch(any());
  }

  @Test
  void newlyStoredImageIsDeletedWhenLaterStorageFails() {
    FacilityRequest facilityRequest = receivedRequest(7L);
    prepareRequest(facilityRequest, List.of());
    MockMultipartFile firstImage = image("first.jpg", "image/jpeg");
    MockMultipartFile secondImage = image("second.jpg", "image/jpeg");
    when(fileStorage.store(firstImage)).thenReturn(storedFile("first.jpg"));
    when(fileStorage.store(secondImage)).thenThrow(
        new FileStorageException("failure", new RuntimeException())
    );

    assertBusinessError(
        () -> updateService.update(
            25L,
            null,
            List.of(firstImage, secondImage),
            7L
        ),
        FacilityRequestErrorCode.FILE_STORAGE_ERROR
    );
    verify(fileStorage).delete("first.jpg");
  }

  private FacilityRequest receivedRequest(Long requesterId) {
    FacilityCategory category = mock(FacilityCategory.class);
    Location location = mock(Location.class);
    AppUser requester = mock(AppUser.class);
    when(requester.getId()).thenReturn(requesterId);
    FacilityRequest facilityRequest = FacilityRequest.create(
        category,
        location,
        requester,
        "Original title",
        "Original description",
        LocalDateTime.of(2026, 8, 1, 16, 0)
    );
    ReflectionTestUtils.setField(facilityRequest, "id", 25L);
    return facilityRequest;
  }

  private void prepareRequest(
      FacilityRequest facilityRequest,
      List<FacilityRequestAttachment> attachments
  ) {
    when(facilityRequestRepository.findDetailById(25L))
        .thenReturn(Optional.of(facilityRequest));
    when(attachmentRepository
        .findAllByFacilityRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(25L))
        .thenReturn(attachments);
  }

  private FacilityRequestAttachment attachment(
      Long fileId,
      String storageKey
  ) {
    FileResource file = mock(FileResource.class);
    when(file.getId()).thenReturn(fileId);
    when(file.getStorageKey()).thenReturn(storageKey);
    FacilityRequestAttachment attachment =
        mock(FacilityRequestAttachment.class);
    when(attachment.getFile()).thenReturn(file);
    return attachment;
  }

  private FacilityRequestUpdateRequest titleUpdate() {
    return new FacilityRequestUpdateRequest(
        null,
        null,
        "Updated title",
        null,
        null
    );
  }

  private MockMultipartFile image(String filename, String contentType) {
    return new MockMultipartFile(
        "files",
        filename,
        contentType,
        "image".getBytes(StandardCharsets.UTF_8)
    );
  }

  private StoredFile storedFile(String storageKey) {
    return new StoredFile(
        "LOCAL",
        storageKey,
        storageKey,
        "image/jpeg",
        5L,
        "checksum"
    );
  }

  private void assertBusinessError(
      Runnable action,
      FacilityRequestErrorCode expectedError
  ) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(expectedError)
        );
  }
}
