package org.swbe.domain.lostitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.swbe.domain.file.repository.FileResourceRepository;
import org.swbe.domain.file.storage.FileStorage;
import org.swbe.domain.file.storage.FileStorageException;
import org.swbe.domain.file.storage.FileStorageRegistry;
import org.swbe.domain.file.storage.StoredFile;
import org.swbe.domain.lostitem.dto.request.ItemClaimCreateRequest;
import org.swbe.domain.lostitem.entity.ClaimStatusHistory;
import org.swbe.domain.lostitem.entity.ItemClaim;
import org.swbe.domain.lostitem.entity.ItemClaimAttachment;
import org.swbe.domain.lostitem.entity.ItemClaimStatus;
import org.swbe.domain.lostitem.entity.ItemStatusHistory;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemStatus;
import org.swbe.domain.lostitem.exception.ItemClaimErrorCode;
import org.swbe.domain.lostitem.exception.StoredItemErrorCode;
import org.swbe.domain.lostitem.repository.ClaimStatusHistoryRepository;
import org.swbe.domain.lostitem.repository.ItemClaimAttachmentRepository;
import org.swbe.domain.lostitem.repository.ItemClaimRepository;
import org.swbe.domain.lostitem.repository.ItemStatusHistoryRepository;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.global.error.BusinessException;

class ItemClaimCreateServiceTest {

  private StoredItemRepository storedItemRepository;
  private ItemClaimRepository itemClaimRepository;
  private ItemClaimAttachmentRepository attachmentRepository;
  private ClaimStatusHistoryRepository claimHistoryRepository;
  private ItemStatusHistoryRepository itemHistoryRepository;
  private AppUserRepository appUserRepository;
  private FileResourceRepository fileResourceRepository;
  private FileStorageRegistry fileStorageRegistry;
  private FileStorage fileStorage;
  private ItemClaimCreateService service;
  private StoredItem storedItem;
  private AppUser claimant;

  @BeforeEach
  void setUp() {
    storedItemRepository = mock(StoredItemRepository.class);
    itemClaimRepository = mock(ItemClaimRepository.class);
    attachmentRepository = mock(ItemClaimAttachmentRepository.class);
    claimHistoryRepository = mock(ClaimStatusHistoryRepository.class);
    itemHistoryRepository = mock(ItemStatusHistoryRepository.class);
    appUserRepository = mock(AppUserRepository.class);
    fileResourceRepository = mock(FileResourceRepository.class);
    fileStorageRegistry = mock(FileStorageRegistry.class);
    fileStorage = mock(FileStorage.class);
    storedItem = mock(StoredItem.class);
    claimant = mock(AppUser.class);

    when(storedItem.getId()).thenReturn(25L);
    when(storedItem.getPublicStatus()).thenReturn(StoredItemStatus.STORED);
    when(storedItem.changeStatus(StoredItemStatus.IN_PROGRESS, now()))
        .thenReturn(true);
    when(storedItemRepository.findByIdForUpdate(25L))
        .thenReturn(Optional.of(storedItem));
    when(appUserRepository.findById(7L))
        .thenReturn(Optional.of(claimant));
    when(claimant.getName()).thenReturn("정석우");
    when(claimant.getStudentNumber()).thenReturn("60251423");
    when(fileStorageRegistry.writeStorage()).thenReturn(fileStorage);
    when(fileStorageRegistry.get("LOCAL")).thenReturn(fileStorage);
    when(itemClaimRepository.save(any(ItemClaim.class)))
        .thenAnswer(invocation -> {
          ItemClaim claim = invocation.getArgument(0);
          ReflectionTestUtils.setField(claim, "id", 31L);
          return claim;
        });

    service = new ItemClaimCreateService(
        storedItemRepository,
        itemClaimRepository,
        attachmentRepository,
        claimHistoryRepository,
        itemHistoryRepository,
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
  void studentCreatesInProgressClaimWithAccountInfoAndImages() {
    when(fileStorage.store(any()))
        .thenReturn(storedFile("proof.jpg", "claims/proof.jpg"));

    var response = service.create(
        25L,
        request(),
        List.of(image("proof.jpg", "image/jpeg")),
        7L
    );

    assertThat(response.data().itemClaimId()).isEqualTo(31L);
    assertThat(response.data().storedItemId()).isEqualTo(25L);
    assertThat(response.data().claimantName()).isEqualTo("정석우");
    assertThat(response.data().studentNumber()).isEqualTo("60251423");
    assertThat(response.data().claimStatus()).isEqualTo("IN_PROGRESS");
    assertThat(response.data().attachmentCount()).isEqualTo(1);
    assertThat(response.data().createdAt()).isEqualTo(now());

    ArgumentCaptor<ItemClaim> claim = ArgumentCaptor.forClass(
        ItemClaim.class
    );
    verify(itemClaimRepository).save(claim.capture());
    assertThat(claim.getValue().getClaimantUser()).isSameAs(claimant);
    assertThat(claim.getValue().getStoredItem()).isSameAs(storedItem);
    assertThat(claim.getValue().getOwnershipDescription())
        .isEqualTo("검은색 지갑 내부 카드 정보를 확인해주세요.");
    assertThat(claim.getValue().getClaimStatus())
        .isEqualTo(ItemClaimStatus.IN_PROGRESS);

    ArgumentCaptor<ClaimStatusHistory> claimHistory =
        ArgumentCaptor.forClass(ClaimStatusHistory.class);
    verify(claimHistoryRepository).save(claimHistory.capture());
    assertThat(claimHistory.getValue().getPreviousStatus()).isNull();
    assertThat(claimHistory.getValue().getNewStatus())
        .isEqualTo(ItemClaimStatus.IN_PROGRESS);
    assertThat(claimHistory.getValue().getChangedBy())
        .isSameAs(claimant);

    verify(attachmentRepository).saveAllAndFlush(anyList());
    verify(storedItem).changeStatus(StoredItemStatus.IN_PROGRESS, now());
    ArgumentCaptor<ItemStatusHistory> itemHistory =
        ArgumentCaptor.forClass(ItemStatusHistory.class);
    verify(itemHistoryRepository).save(itemHistory.capture());
    assertThat(itemHistory.getValue().getPreviousStatus())
        .isEqualTo(StoredItemStatus.STORED);
    assertThat(itemHistory.getValue().getNewStatus())
        .isEqualTo(StoredItemStatus.IN_PROGRESS);
  }

  @Test
  void claimOnAlreadyInProgressItemDoesNotDuplicateItemHistory() {
    when(storedItem.getPublicStatus())
        .thenReturn(StoredItemStatus.IN_PROGRESS);
    when(storedItem.changeStatus(StoredItemStatus.IN_PROGRESS, now()))
        .thenReturn(false);

    service.create(25L, request(), List.of(), 7L);

    verify(itemHistoryRepository, never()).save(any());
    verify(attachmentRepository, never()).saveAllAndFlush(anyList());
  }

  @Test
  void duplicateActiveClaimIsRejected() {
    when(itemClaimRepository
        .existsByStoredItem_IdAndClaimantUser_IdAndClaimStatusIn(
            25L,
            7L,
            ItemClaimStatus.activeStatuses()
        )).thenReturn(true);

    assertBusinessError(
        () -> service.create(25L, request(), List.of(), 7L),
        ItemClaimErrorCode.DUPLICATE_ACTIVE_CLAIM
    );
    verify(itemClaimRepository, never()).save(any());
  }

  @Test
  void completedStoredItemCannotReceiveClaim() {
    when(storedItem.getPublicStatus())
        .thenReturn(StoredItemStatus.COMPLETED);

    assertBusinessError(
        () -> service.create(25L, request(), List.of(), 7L),
        ItemClaimErrorCode.NOT_CLAIMABLE
    );
    verify(appUserRepository, never()).findById(any());
  }

  @Test
  void missingStoredItemReturnsNotFound() {
    when(storedItemRepository.findByIdForUpdate(99L))
        .thenReturn(Optional.empty());

    assertBusinessError(
        () -> service.create(99L, request(), List.of(), 7L),
        StoredItemErrorCode.NOT_FOUND
    );
  }

  @Test
  void moreThanFiveImagesIsRejectedBeforeDatabaseAccess() {
    List<MultipartFile> images = List.of(
        image("1.jpg", "image/jpeg"),
        image("2.jpg", "image/jpeg"),
        image("3.jpg", "image/jpeg"),
        image("4.jpg", "image/jpeg"),
        image("5.jpg", "image/jpeg"),
        image("6.jpg", "image/jpeg")
    );

    assertBusinessError(
        () -> service.create(25L, request(), images, 7L),
        ItemClaimErrorCode.FILE_LIMIT_EXCEEDED
    );
    verify(storedItemRepository, never()).findByIdForUpdate(any());
  }

  @Test
  void nonImageAttachmentIsRejected() {
    assertBusinessError(
        () -> service.create(
            25L,
            request(),
            List.of(image("proof.pdf", "application/pdf")),
            7L
        ),
        ItemClaimErrorCode.INVALID_FILE_TYPE
    );
  }

  @Test
  void previouslyStoredImageIsDeletedWhenLaterUploadFails() {
    when(fileStorage.store(any()))
        .thenReturn(storedFile("first.jpg", "claims/first.jpg"))
        .thenThrow(new FileStorageException(
            "upload failed",
            new IllegalStateException()
        ));

    assertBusinessError(
        () -> service.create(
            25L,
            request(),
            List.of(
                image("first.jpg", "image/jpeg"),
                image("second.jpg", "image/jpeg")
            ),
            7L
        ),
        ItemClaimErrorCode.FILE_STORAGE_ERROR
    );
    verify(fileStorage).delete("claims/first.jpg");
  }

  private ItemClaimCreateRequest request() {
    return new ItemClaimCreateRequest(
        " 검은색 지갑 내부 카드 정보를 확인해주세요. "
    );
  }

  private MockMultipartFile image(
      String filename,
      String contentType
  ) {
    return new MockMultipartFile(
        "files",
        filename,
        contentType,
        "image".getBytes()
    );
  }

  private StoredFile storedFile(String filename, String storageKey) {
    return new StoredFile(
        "LOCAL",
        storageKey,
        filename,
        "image/jpeg",
        5L,
        "checksum"
    );
  }

  private LocalDateTime now() {
    return LocalDateTime.of(2026, 8, 12, 5, 30);
  }

  private void assertBusinessError(
      Runnable action,
      org.swbe.global.error.ErrorCode expectedError
  ) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessException.class)
        .extracting(exception ->
            ((BusinessException) exception).getErrorCode()
        )
        .isEqualTo(expectedError);
  }
}
