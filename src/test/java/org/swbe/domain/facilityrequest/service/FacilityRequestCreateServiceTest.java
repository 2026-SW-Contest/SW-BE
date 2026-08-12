package org.swbe.domain.facilityrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.campus.repository.LocationRepository;
import org.swbe.domain.facilityrequest.dto.request.FacilityRequestCreateRequest;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestCreateResponse;
import org.swbe.domain.facilityrequest.entity.FacilityCategory;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.exception.FacilityRequestErrorCode;
import org.swbe.domain.facilityrequest.repository.FacilityCategoryRepository;
import org.swbe.domain.facilityrequest.repository.FacilityRequestAttachmentRepository;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.domain.file.repository.FileResourceRepository;
import org.swbe.domain.file.storage.FileStorage;
import org.swbe.domain.file.storage.FileStorageException;
import org.swbe.domain.file.storage.FileStorageRegistry;
import org.swbe.domain.file.storage.StoredFile;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.global.error.BusinessException;

@ExtendWith(MockitoExtension.class)
class FacilityRequestCreateServiceTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(
      Instant.parse("2026-08-01T07:00:00Z"),
      ZoneOffset.UTC
  );

  @Mock
  private FacilityRequestRepository facilityRequestRepository;

  @Mock
  private FacilityRequestAttachmentRepository attachmentRepository;

  @Mock
  private FacilityCategoryRepository facilityCategoryRepository;

  @Mock
  private LocationRepository locationRepository;

  @Mock
  private AppUserRepository appUserRepository;

  @Mock
  private FileResourceRepository fileResourceRepository;

  @Mock
  private FileStorage fileStorage;

  @Mock
  private FileStorageRegistry fileStorageRegistry;

  private FacilityRequestCreateService createService;

  @BeforeEach
  void setUp() {
    lenient().when(fileStorageRegistry.writeStorage())
        .thenReturn(fileStorage);
    lenient().when(fileStorageRegistry.get("LOCAL"))
        .thenReturn(fileStorage);
    createService = new FacilityRequestCreateService(
        facilityRequestRepository,
        attachmentRepository,
        facilityCategoryRepository,
        locationRepository,
        appUserRepository,
        fileResourceRepository,
        fileStorageRegistry,
        FIXED_CLOCK
    );
  }

  @Test
  void createsFacilityRequestWithImage() {
    FacilityCategory category = mock(FacilityCategory.class);
    Location location = mock(Location.class);
    AppUser requester = mock(AppUser.class);
    MockMultipartFile image = image("broken-light.jpg", "image/jpeg");
    StoredFile storedFile = new StoredFile(
        "LOCAL",
        "2026/08/01/image.jpg",
        "broken-light.jpg",
        "image/jpeg",
        image.getSize(),
        "checksum"
    );
    when(facilityCategoryRepository.findByIdAndActiveTrue(1L))
        .thenReturn(Optional.of(category));
    when(locationRepository
        .findByIdAndActiveTrueAndBuilding_ActiveTrue(2L))
        .thenReturn(Optional.of(location));
    when(appUserRepository.findById(7L)).thenReturn(Optional.of(requester));
    when(fileStorage.store(image)).thenReturn(storedFile);
    when(facilityRequestRepository.save(any())).thenAnswer(invocation -> {
      FacilityRequest facilityRequest = invocation.getArgument(0);
      ReflectionTestUtils.setField(facilityRequest, "id", 25L);
      return facilityRequest;
    });

    FacilityRequestCreateResponse response = createService.create(
        validRequest(),
        List.of(image),
        7L
    );

    assertThat(response.data().facilityRequestId()).isEqualTo(25L);
    assertThat(response.data().requestStatus()).isEqualTo("RECEIVED");
    assertThat(response.data().attachmentCount()).isEqualTo(1);

    ArgumentCaptor<FacilityRequest> requestCaptor =
        ArgumentCaptor.forClass(FacilityRequest.class);
    verify(facilityRequestRepository).save(requestCaptor.capture());
    FacilityRequest savedRequest = requestCaptor.getValue();
    assertThat(savedRequest.getFacilityCategory()).isSameAs(category);
    assertThat(savedRequest.getLocation()).isSameAs(location);
    assertThat(savedRequest.getRequester()).isSameAs(requester);
    assertThat(savedRequest.getTitle()).isEqualTo("Flickering hallway light");
    assertThat(savedRequest.getDescription())
        .isEqualTo("The hallway light keeps flickering.");
    verify(fileResourceRepository).save(any());
    verify(attachmentRepository).saveAll(any());
  }

  @Test
  void moreThanFiveImagesAreRejected() {
    List<MultipartFile> files = List.of(
        image("1.jpg", "image/jpeg"),
        image("2.jpg", "image/jpeg"),
        image("3.jpg", "image/jpeg"),
        image("4.jpg", "image/jpeg"),
        image("5.jpg", "image/jpeg"),
        image("6.jpg", "image/jpeg")
    );

    assertThatThrownBy(() -> createService.create(
        validRequest(),
        files,
        7L
    )).isInstanceOfSatisfying(BusinessException.class, exception ->
        assertThat(exception.getErrorCode()).isEqualTo(
            FacilityRequestErrorCode.FILE_LIMIT_EXCEEDED
        ));

    verifyNoInteractions(facilityRequestRepository);
  }

  @Test
  void nonImageFileIsRejected() {
    MockMultipartFile textFile = image("note.txt", "text/plain");

    assertThatThrownBy(() -> createService.create(
        validRequest(),
        List.of(textFile),
        7L
    )).isInstanceOfSatisfying(BusinessException.class, exception ->
        assertThat(exception.getErrorCode()).isEqualTo(
            FacilityRequestErrorCode.INVALID_FILE_TYPE
        ));

    verifyNoInteractions(facilityRequestRepository);
  }

  @Test
  void inactiveCategoryIsRejected() {
    when(facilityCategoryRepository.findByIdAndActiveTrue(1L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> createService.create(
        validRequest(),
        List.of(),
        7L
    )).isInstanceOfSatisfying(BusinessException.class, exception ->
        assertThat(exception.getErrorCode()).isEqualTo(
            FacilityRequestErrorCode.CATEGORY_NOT_FOUND
        ));

    verifyNoInteractions(locationRepository);
  }

  @Test
  void storageFailureReturnsFileStorageError() {
    prepareReferences();
    MockMultipartFile image = image("broken-light.jpg", "image/jpeg");
    when(fileStorage.store(image)).thenThrow(
        new FileStorageException("failure", new RuntimeException())
    );

    assertThatThrownBy(() -> createService.create(
        validRequest(),
        List.of(image),
        7L
    )).isInstanceOfSatisfying(BusinessException.class, exception ->
        assertThat(exception.getErrorCode()).isEqualTo(
            FacilityRequestErrorCode.FILE_STORAGE_ERROR
        ));
  }

  @Test
  void storedFileIsDeletedWhenDatabaseSaveFails() {
    prepareReferences();
    MockMultipartFile image = image("broken-light.jpg", "image/jpeg");
    StoredFile storedFile = new StoredFile(
        "LOCAL",
        "2026/08/01/image.jpg",
        "broken-light.jpg",
        "image/jpeg",
        image.getSize(),
        "checksum"
    );
    when(fileStorage.store(image)).thenReturn(storedFile);
    doThrow(new RuntimeException("database failure"))
        .when(attachmentRepository).saveAll(any());

    assertThatThrownBy(() -> createService.create(
        validRequest(),
        List.of(image),
        7L
    )).isInstanceOf(RuntimeException.class)
        .hasMessage("database failure");

    verify(fileStorage).delete("2026/08/01/image.jpg");
  }

  private void prepareReferences() {
    when(facilityCategoryRepository.findByIdAndActiveTrue(1L))
        .thenReturn(Optional.of(mock(FacilityCategory.class)));
    when(locationRepository
        .findByIdAndActiveTrueAndBuilding_ActiveTrue(2L))
        .thenReturn(Optional.of(mock(Location.class)));
    when(appUserRepository.findById(7L))
        .thenReturn(Optional.of(mock(AppUser.class)));
  }

  private FacilityRequestCreateRequest validRequest() {
    return new FacilityRequestCreateRequest(
        1L,
        2L,
        "Flickering hallway light",
        "The hallway light keeps flickering."
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
}
