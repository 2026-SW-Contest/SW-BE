package org.swbe.domain.lostitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.campus.repository.LocationRepository;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.repository.FileResourceRepository;
import org.swbe.domain.file.storage.FileStorage;
import org.swbe.domain.file.storage.FileStorageRegistry;
import org.swbe.domain.file.storage.StoredFile;
import org.swbe.domain.lostitem.dto.request.StoredItemUpdateRequest;
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

class StoredItemUpdateServiceTest {

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
  private StoredItemUpdateService service;
  private StoredItem item;
  private AppUser updater;

  @BeforeEach
  void setUp() {
    TransactionSynchronizationManager.initSynchronization();
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
    updater = mock(AppUser.class);
    item = storedItem();
    when(storedItemRepository.findDetailById(25L))
        .thenReturn(Optional.of(item));
    when(assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(3L, 7L))
        .thenReturn(true);
    when(appUserRepository.findById(7L)).thenReturn(Optional.of(updater));
    when(fileStorageRegistry.writeStorage()).thenReturn(fileStorage);
    when(fileStorageRegistry.get("S3")).thenReturn(fileStorage);
    when(attachmentRepository.saveAll(anyList()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    service = new StoredItemUpdateService(
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
            Instant.parse("2026-08-12T06:30:00Z"),
            ZoneOffset.UTC
        )
    );
  }

  @AfterEach
  void tearDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void reordersRetainedImagesAndAppendsNewImage() {
    StoredItemAttachment first = attachment(31L, "first.jpg", 0);
    StoredItemAttachment second = attachment(32L, "second.jpg", 1);
    when(attachmentRepository.findPublicImagesByStoredItemId(25L))
        .thenReturn(List.of(first, second));
    when(fileStorage.store(any())).thenReturn(
        new StoredFile(
            "S3",
            "stored/new.jpg",
            "new.jpg",
            "image/jpeg",
            5L,
            "checksum"
        )
    );
    StoredItemUpdateRequest request = new StoredItemUpdateRequest();
    request.setKeepFileIds(List.of(32L, 31L));

    var response = service.update(
        25L,
        request,
        List.of(image()),
        7L,
        false
    );

    assertThat(response.data().attachmentCount()).isEqualTo(3);
    assertThat(second.isPrimary()).isTrue();
    assertThat(second.getDisplayOrder()).isZero();
    assertThat(first.isPrimary()).isFalse();
    assertThat(first.getDisplayOrder()).isEqualTo(1);
    ArgumentCaptor<List<StoredItemAttachment>> added = listCaptor();
    verify(attachmentRepository).saveAll(added.capture());
    assertThat(added.getValue()).singleElement()
        .satisfies(attachment -> {
          assertThat(attachment.isPrimary()).isFalse();
          assertThat(attachment.getDisplayOrder()).isEqualTo(2);
        });
  }

  @Test
  void updatesLocationWithOptionalTextAndClearsPrivateDescription() {
    Location updatedLocation = mock(Location.class);
    when(locationRepository
        .findByIdAndActiveTrueAndBuilding_ActiveTrue(10L))
        .thenReturn(Optional.of(updatedLocation));
    when(attachmentRepository.findPublicImagesByStoredItemId(25L))
        .thenReturn(List.of());
    StoredItemUpdateRequest request = new StoredItemUpdateRequest();
    request.setFoundLocationId(10L);
    request.setFoundLocationText(" 명진관 앞 벤치 ");
    request.setPrivateDescription("");
    request.setItemName(" 수정된 지갑 ");

    service.update(25L, request, List.of(), 7L, false);

    assertThat(item.getFoundLocation()).isSameAs(updatedLocation);
    assertThat(item.getFoundLocationText()).isEqualTo("명진관 앞 벤치");
    assertThat(item.getPrivateDescription()).isNull();
    assertThat(item.getItemName()).isEqualTo("수정된 지갑");
    assertThat(item.getUpdatedAt()).isEqualTo(
        LocalDateTime.of(2026, 8, 12, 6, 30)
    );
  }

  @Test
  void staffMustBeAssignedToCurrentAndTargetOffices() {
    LostItemOffice targetOffice = mock(LostItemOffice.class);
    when(targetOffice.getId()).thenReturn(4L);
    when(officeRepository.findByIdAndActiveTrue(4L))
        .thenReturn(Optional.of(targetOffice));
    when(assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(4L, 7L))
        .thenReturn(false);
    StoredItemUpdateRequest request = new StoredItemUpdateRequest();
    request.setOfficeId(4L);

    assertThatThrownBy(() -> service.update(
        25L,
        request,
        List.of(),
        7L,
        false
    )).isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode().code()
        ).isEqualTo("STORED_ITEM_ACCESS_DENIED"));
  }

  @Test
  void adminCanMoveItemWithoutOfficeAssignments() {
    LostItemOffice targetOffice = mock(LostItemOffice.class);
    when(targetOffice.getId()).thenReturn(4L);
    when(officeRepository.findByIdAndActiveTrue(4L))
        .thenReturn(Optional.of(targetOffice));
    when(attachmentRepository.findPublicImagesByStoredItemId(25L))
        .thenReturn(List.of());
    StoredItemUpdateRequest request = new StoredItemUpdateRequest();
    request.setOfficeId(4L);

    service.update(25L, request, List.of(), 7L, true);

    assertThat(item.getOffice()).isSameAs(targetOffice);
    verify(assignmentRepository, never())
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(any(), any());
  }

  @Test
  void rejectsLocationTextWithoutLocationId() {
    StoredItemUpdateRequest request = new StoredItemUpdateRequest();
    request.setFoundLocationText("명진관 앞 벤치");

    assertThatThrownBy(() -> service.update(
        25L,
        request,
        List.of(),
        7L,
        false
    )).isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode().code()
        ).isEqualTo("STORED_ITEM_INVALID_FOUND_LOCATION"));
    verify(storedItemRepository, never()).findDetailById(any());
  }

  @Test
  void locationIdWithoutTextClearsExistingLocationText() {
    Location updatedLocation = mock(Location.class);
    when(locationRepository
        .findByIdAndActiveTrueAndBuilding_ActiveTrue(10L))
        .thenReturn(Optional.of(updatedLocation));
    when(attachmentRepository.findPublicImagesByStoredItemId(25L))
        .thenReturn(List.of());
    StoredItemUpdateRequest request = new StoredItemUpdateRequest();
    request.setFoundLocationId(10L);

    service.update(25L, request, List.of(), 7L, false);

    assertThat(item.getFoundLocation()).isSameAs(updatedLocation);
    assertThat(item.getFoundLocationText()).isNull();
  }

  @Test
  void rejectsFileIdFromAnotherItem() {
    StoredItemAttachment existing = attachment(31L, "first.jpg", 0);
    when(attachmentRepository.findPublicImagesByStoredItemId(25L))
        .thenReturn(List.of(existing));
    StoredItemUpdateRequest request = new StoredItemUpdateRequest();
    request.setKeepFileIds(List.of(99L));

    assertThatThrownBy(() -> service.update(
        25L,
        request,
        List.of(),
        7L,
        false
    )).isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode().code()
        ).isEqualTo("STORED_ITEM_INVALID_ATTACHMENT"));
  }

  @Test
  void rejectsMoreThanFiveFinalImages() {
    List<StoredItemAttachment> existing = List.of(
        attachment(31L, "1.jpg", 0),
        attachment(32L, "2.jpg", 1),
        attachment(33L, "3.jpg", 2),
        attachment(34L, "4.jpg", 3),
        attachment(35L, "5.jpg", 4)
    );
    when(attachmentRepository.findPublicImagesByStoredItemId(25L))
        .thenReturn(existing);

    assertThatThrownBy(() -> service.update(
        25L,
        new StoredItemUpdateRequest(),
        List.of(image()),
        7L,
        false
    )).isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode().code()
        ).isEqualTo("STORED_ITEM_FILE_LIMIT_EXCEEDED"));
  }

  @Test
  void deletesRemovedDatabaseRowsBeforeDeletingObjectAfterCommit() {
    StoredItemAttachment removed = attachment(31L, "first.jpg", 0);
    FileResource removedFile = removed.getFile();
    when(attachmentRepository.findPublicImagesByStoredItemId(25L))
        .thenReturn(List.of(removed));
    StoredItemUpdateRequest request = new StoredItemUpdateRequest();
    request.setKeepFileIds(List.of());

    service.update(25L, request, List.of(), 7L, false);

    InOrder databaseOrder = inOrder(
        attachmentRepository,
        fileResourceRepository
    );
    databaseOrder.verify(attachmentRepository)
        .deleteAll(List.of(removed));
    databaseOrder.verify(attachmentRepository).flush();
    databaseOrder.verify(fileResourceRepository)
        .deleteAll(List.of(removedFile));
    databaseOrder.verify(fileResourceRepository).flush();
    verify(fileStorage, never()).delete("stored/first.jpg");

    TransactionSynchronizationManager.getSynchronizations()
        .forEach(TransactionSynchronization::afterCommit);

    verify(fileStorage).delete("stored/first.jpg");
  }

  @Test
  void cleansNewObjectAndMapsOptimisticLockConflict() {
    when(attachmentRepository.findPublicImagesByStoredItemId(25L))
        .thenReturn(List.of());
    when(fileStorage.store(any())).thenReturn(
        new StoredFile(
            "S3",
            "stored/new.jpg",
            "new.jpg",
            "image/jpeg",
            5L,
            "checksum"
        )
    );
    org.mockito.Mockito.doThrow(
        new OptimisticLockingFailureException("conflict")
    ).when(storedItemRepository).flush();

    assertThatThrownBy(() -> service.update(
        25L,
        null,
        List.of(image()),
        7L,
        false
    )).isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode().code()
        ).isEqualTo("STORED_ITEM_VERSION_CONFLICT"));
    verify(fileStorage).delete("stored/new.jpg");
  }

  private StoredItem storedItem() {
    LostItemOffice office = mock(LostItemOffice.class);
    when(office.getId()).thenReturn(3L);
    ItemCategory category = mock(ItemCategory.class);
    Location location = mock(Location.class);
    AppUser registrant = mock(AppUser.class);
    StoredItem result = StoredItem.create(
        office,
        location,
        "기존 상세 위치",
        registrant,
        category,
        "검은색 지갑",
        "공개 설명",
        "내부 설명",
        LocalDate.of(2026, 8, 10),
        LocalDateTime.of(2026, 8, 10, 14, 30)
    );
    ReflectionTestUtils.setField(result, "id", 25L);
    return result;
  }

  private StoredItemAttachment attachment(
      Long fileId,
      String filename,
      int order
  ) {
    FileResource file = mock(FileResource.class);
    when(file.getId()).thenReturn(fileId);
    when(file.getOriginalFilename()).thenReturn(filename);
    when(file.getStorageProvider()).thenReturn("S3");
    when(file.getStorageKey()).thenReturn("stored/" + filename);
    return StoredItemAttachment.attach(item, file, order == 0, order);
  }

  private MockMultipartFile image() {
    return new MockMultipartFile(
        "files",
        "new.jpg",
        "image/jpeg",
        "image".getBytes()
    );
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ArgumentCaptor<List<StoredItemAttachment>> listCaptor() {
    return ArgumentCaptor.forClass((Class) List.class);
  }
}
