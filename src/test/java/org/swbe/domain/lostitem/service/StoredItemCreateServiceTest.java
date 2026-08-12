package org.swbe.domain.lostitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.campus.repository.LocationRepository;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.repository.FileResourceRepository;
import org.swbe.domain.file.storage.FileStorage;
import org.swbe.domain.file.storage.FileStorageException;
import org.swbe.domain.file.storage.FileStorageRegistry;
import org.swbe.domain.file.storage.StoredFile;
import org.swbe.domain.lostitem.dto.request.StoredItemCreateRequest;
import org.swbe.domain.lostitem.entity.ItemCategory;
import org.swbe.domain.lostitem.entity.LostItemOffice;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemAttachment;
import org.swbe.domain.lostitem.repository.ItemCategoryRepository;
import org.swbe.domain.lostitem.repository.LostItemOfficeRepository;
import org.swbe.domain.lostitem.repository.OfficeStaffAssignmentRepository;
import org.swbe.domain.lostitem.repository.StoredItemAttachmentRepository;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.global.error.BusinessException;

class StoredItemCreateServiceTest {

  private StoredItemRepository storedItemRepository;
  private StoredItemAttachmentRepository attachmentRepository;
  private LostItemOfficeRepository officeRepository;
  private OfficeStaffAssignmentRepository assignmentRepository;
  private ItemCategoryRepository itemCategoryRepository;
  private LocationRepository locationRepository;
  private AppUserRepository appUserRepository;
  private FileResourceRepository fileResourceRepository;
  private FileStorageRegistry fileStorageRegistry;
  private FileStorage fileStorage;
  private StoredItemCreateService service;
  private LostItemOffice office;
  private ItemCategory category;
  private Location location;
  private AppUser user;

  @BeforeEach
  void setUp() {
    storedItemRepository = mock(StoredItemRepository.class);
    attachmentRepository = mock(StoredItemAttachmentRepository.class);
    officeRepository = mock(LostItemOfficeRepository.class);
    assignmentRepository = mock(OfficeStaffAssignmentRepository.class);
    itemCategoryRepository = mock(ItemCategoryRepository.class);
    locationRepository = mock(LocationRepository.class);
    appUserRepository = mock(AppUserRepository.class);
    fileResourceRepository = mock(FileResourceRepository.class);
    fileStorageRegistry = mock(FileStorageRegistry.class);
    fileStorage = mock(FileStorage.class);
    office = mock(LostItemOffice.class);
    category = mock(ItemCategory.class);
    location = mock(Location.class);
    user = mock(AppUser.class);
    when(office.getId()).thenReturn(3L);
    when(officeRepository.findByIdAndActiveTrue(3L))
        .thenReturn(Optional.of(office));
    when(itemCategoryRepository.findById(2L))
        .thenReturn(Optional.of(category));
    when(locationRepository
        .findByIdAndActiveTrueAndBuilding_ActiveTrue(10L))
        .thenReturn(Optional.of(location));
    when(appUserRepository.findById(7L)).thenReturn(Optional.of(user));
    when(fileStorageRegistry.writeStorage()).thenReturn(fileStorage);
    when(fileStorageRegistry.get("LOCAL")).thenReturn(fileStorage);
    when(storedItemRepository.save(any(StoredItem.class)))
        .thenAnswer(invocation -> {
          StoredItem item = invocation.getArgument(0);
          ReflectionTestUtils.setField(item, "id", 25L);
          return item;
        });
    service = new StoredItemCreateService(
        storedItemRepository,
        attachmentRepository,
        officeRepository,
        assignmentRepository,
        itemCategoryRepository,
        locationRepository,
        appUserRepository,
        fileResourceRepository,
        fileStorageRegistry,
        Clock.fixed(
            Instant.parse("2026-08-12T05:30:00Z"),
            ZoneOffset.UTC
        )
    );
  }

  @Test
  void assignedStaffCreatesItemWithOrderedImages() {
    when(assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(3L, 7L))
        .thenReturn(true);
    when(fileStorage.store(any()))
        .thenReturn(
            storedFile("first.jpg", "stored/first.jpg"),
            storedFile("second.png", "stored/second.png")
        );

    var response = service.create(
        requestWithLocationId(),
        List.of(image("first.jpg"), image("second.png")),
        7L,
        false
    );

    assertThat(response.data().storedItemId()).isEqualTo(25L);
    assertThat(response.data().publicStatus()).isEqualTo("STORED");
    assertThat(response.data().attachmentCount()).isEqualTo(2);
    ArgumentCaptor<List<StoredItemAttachment>> attachments =
        listCaptor();
    verify(attachmentRepository).saveAllAndFlush(
        attachments.capture()
    );
    assertThat(attachments.getValue()).hasSize(2);
    assertThat(attachments.getValue().getFirst().isPrimary()).isTrue();
    assertThat(attachments.getValue().getFirst().getDisplayOrder())
        .isZero();
    assertThat(attachments.getValue().get(1).isPrimary()).isFalse();
    assertThat(attachments.getValue().get(1).getDisplayOrder())
        .isEqualTo(1);
    ArgumentCaptor<StoredItem> item = ArgumentCaptor.forClass(
        StoredItem.class
    );
    verify(storedItemRepository).save(item.capture());
    assertThat(item.getValue().getFoundLocation()).isSameAs(location);
    assertThat(item.getValue().getFoundLocationText()).isNull();
    assertThat(item.getValue().getStorageDeadline()).isNull();
    assertThat(item.getValue().getStoragePosition()).isNull();
    assertThat(item.getValue().getFoundTime()).isNull();
    assertThat(item.getValue().isFoundTimeUnknown()).isTrue();
  }

  @Test
  void adminCanCreateForAnyActiveOfficeWithFreeTextLocation() {
    var response = service.create(
        requestWithFreeTextLocation(),
        List.of(),
        7L,
        true
    );

    assertThat(response.data().storedItemId()).isEqualTo(25L);
    verifyNoInteractions(assignmentRepository, locationRepository);
    ArgumentCaptor<StoredItem> item = ArgumentCaptor.forClass(
        StoredItem.class
    );
    verify(storedItemRepository).save(item.capture());
    assertThat(item.getValue().getFoundLocation()).isNull();
    assertThat(item.getValue().getFoundLocationText())
        .isEqualTo("명진관 앞 벤치");
  }

  @Test
  void staffWithoutActiveAssignmentIsDenied() {
    when(assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(3L, 7L))
        .thenReturn(false);

    assertThatThrownBy(() -> service.create(
        requestWithLocationId(),
        List.of(),
        7L,
        false
    )).isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode().code()
        ).isEqualTo("STORED_ITEM_ACCESS_DENIED"));
    verify(storedItemRepository, never()).save(any());
  }

  @Test
  void requiresExactlyOneFoundLocation() {
    StoredItemCreateRequest invalid = new StoredItemCreateRequest(
        3L,
        2L,
        10L,
        "명진관 앞 벤치",
        "검은색 지갑",
        "공개 설명",
        null,
        LocalDate.of(2026, 8, 12)
    );

    assertThatThrownBy(() -> service.create(
        invalid,
        List.of(),
        7L,
        false
    )).isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode().code()
        ).isEqualTo("STORED_ITEM_INVALID_FOUND_LOCATION"));
    verifyNoInteractions(officeRepository);
  }

  @Test
  void rejectsMoreThanFiveImages() {
    List<MultipartFile> files = List.of(
        image("1.jpg"),
        image("2.jpg"),
        image("3.jpg"),
        image("4.jpg"),
        image("5.jpg"),
        image("6.jpg")
    );

    assertThatThrownBy(() -> service.create(
        requestWithLocationId(),
        files,
        7L,
        false
    )).isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode().code()
        ).isEqualTo("STORED_ITEM_FILE_LIMIT_EXCEEDED"));
  }

  @Test
  void deletesAlreadyStoredObjectWhenLaterUploadFails() {
    when(assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(3L, 7L))
        .thenReturn(true);
    StoredFile first = storedFile("first.jpg", "stored/first.jpg");
    when(fileStorage.store(any()))
        .thenReturn(first)
        .thenThrow(new FileStorageException(
            "upload failed",
            new IllegalStateException()
        ));

    assertThatThrownBy(() -> service.create(
        requestWithLocationId(),
        List.of(image("first.jpg"), image("second.jpg")),
        7L,
        false
    )).isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode().code()
        ).isEqualTo("STORED_ITEM_FILE_STORAGE_ERROR"));
    verify(fileStorage).delete("stored/first.jpg");
  }

  @Test
  void deletesStoredObjectWhenDatabaseFlushFails() {
    when(assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(3L, 7L))
        .thenReturn(true);
    when(fileStorage.store(any()))
        .thenReturn(storedFile("first.jpg", "stored/first.jpg"));
    when(attachmentRepository.saveAllAndFlush(anyList()))
        .thenThrow(new IllegalStateException("database failed"));

    assertThatThrownBy(() -> service.create(
        requestWithLocationId(),
        List.of(image("first.jpg")),
        7L,
        false
    )).isInstanceOf(IllegalStateException.class);
    verify(fileStorage).delete("stored/first.jpg");
  }

  private StoredItemCreateRequest requestWithLocationId() {
    return new StoredItemCreateRequest(
        3L,
        2L,
        10L,
        null,
        "검은색 지갑",
        "공개 설명",
        "내부 설명",
        LocalDate.of(2026, 8, 12)
    );
  }

  private StoredItemCreateRequest requestWithFreeTextLocation() {
    return new StoredItemCreateRequest(
        3L,
        2L,
        null,
        " 명진관 앞 벤치 ",
        "검은색 지갑",
        "공개 설명",
        null,
        LocalDate.of(2026, 8, 12)
    );
  }

  private MockMultipartFile image(String filename) {
    return new MockMultipartFile(
        "files",
        filename,
        filename.endsWith(".png") ? "image/png" : "image/jpeg",
        "image".getBytes()
    );
  }

  private StoredFile storedFile(String filename, String storageKey) {
    return new StoredFile(
        "LOCAL",
        storageKey,
        filename,
        filename.endsWith(".png") ? "image/png" : "image/jpeg",
        5L,
        "checksum"
    );
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ArgumentCaptor<List<StoredItemAttachment>> listCaptor() {
    return ArgumentCaptor.forClass((Class) List.class);
  }
}
