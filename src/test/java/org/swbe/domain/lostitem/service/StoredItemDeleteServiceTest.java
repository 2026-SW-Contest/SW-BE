package org.swbe.domain.lostitem.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.repository.FileResourceRepository;
import org.swbe.domain.file.storage.FileStorage;
import org.swbe.domain.file.storage.FileStorageException;
import org.swbe.domain.file.storage.FileStorageRegistry;
import org.swbe.domain.lostitem.entity.ItemStatusHistory;
import org.swbe.domain.lostitem.entity.LostItemOffice;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemAttachment;
import org.swbe.domain.lostitem.entity.StoredItemStatus;
import org.swbe.domain.lostitem.exception.StoredItemErrorCode;
import org.swbe.domain.lostitem.repository.ItemClaimRepository;
import org.swbe.domain.lostitem.repository.ItemStatusHistoryRepository;
import org.swbe.domain.lostitem.repository.OfficeStaffAssignmentRepository;
import org.swbe.domain.lostitem.repository.StoredItemAttachmentRepository;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.global.error.BusinessException;

class StoredItemDeleteServiceTest {

  private StoredItemRepository storedItemRepository;
  private StoredItemAttachmentRepository attachmentRepository;
  private FileResourceRepository fileResourceRepository;
  private ItemStatusHistoryRepository statusHistoryRepository;
  private ItemClaimRepository itemClaimRepository;
  private OfficeStaffAssignmentRepository assignmentRepository;
  private FileStorageRegistry fileStorageRegistry;
  private FileStorage fileStorage;
  private StoredItemDeleteService service;
  private StoredItem item;

  @BeforeEach
  void setUp() {
    TransactionSynchronizationManager.initSynchronization();
    storedItemRepository = mock(StoredItemRepository.class);
    attachmentRepository = mock(StoredItemAttachmentRepository.class);
    fileResourceRepository = mock(FileResourceRepository.class);
    statusHistoryRepository = mock(ItemStatusHistoryRepository.class);
    itemClaimRepository = mock(ItemClaimRepository.class);
    assignmentRepository = mock(OfficeStaffAssignmentRepository.class);
    fileStorageRegistry = mock(FileStorageRegistry.class);
    fileStorage = mock(FileStorage.class);
    item = storedItem(StoredItemStatus.STORED);
    when(storedItemRepository.findDetailById(25L))
        .thenReturn(Optional.of(item));
    when(assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(3L, 7L))
        .thenReturn(true);
    when(fileStorageRegistry.get("S3")).thenReturn(fileStorage);
    service = new StoredItemDeleteService(
        storedItemRepository,
        attachmentRepository,
        fileResourceRepository,
        statusHistoryRepository,
        itemClaimRepository,
        assignmentRepository,
        fileStorageRegistry
    );
  }

  @AfterEach
  void tearDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void assignedStaffDeletesStoredItemInForeignKeyOrder() {
    StoredItemAttachment attachment = mock(StoredItemAttachment.class);
    FileResource file = file("S3", "2026/08/12/item.jpg");
    ItemStatusHistory history = mock(ItemStatusHistory.class);
    when(attachment.getFile()).thenReturn(file);
    when(attachmentRepository
        .findAllByStoredItem_IdOrderByDisplayOrderAscIdAsc(25L))
        .thenReturn(List.of(attachment));
    when(statusHistoryRepository
        .findAllByStoredItem_IdOrderByIdAsc(25L))
        .thenReturn(List.of(history));

    service.delete(25L, 7L, false);

    InOrder databaseOrder = inOrder(
        attachmentRepository,
        fileResourceRepository,
        statusHistoryRepository,
        storedItemRepository
    );
    databaseOrder.verify(attachmentRepository)
        .deleteAll(List.of(attachment));
    databaseOrder.verify(attachmentRepository).flush();
    databaseOrder.verify(fileResourceRepository)
        .deleteAll(List.of(file));
    databaseOrder.verify(fileResourceRepository).flush();
    databaseOrder.verify(statusHistoryRepository)
        .deleteAll(List.of(history));
    databaseOrder.verify(statusHistoryRepository).flush();
    databaseOrder.verify(storedItemRepository).delete(item);
    databaseOrder.verify(storedItemRepository).flush();
    verify(fileStorage, never()).delete("2026/08/12/item.jpg");

    runAfterCommitCallbacks();

    verify(fileStorage).delete("2026/08/12/item.jpg");
  }

  @Test
  void storedItemWithoutAttachmentsCanBeDeleted() {
    ItemStatusHistory history = mock(ItemStatusHistory.class);
    when(attachmentRepository
        .findAllByStoredItem_IdOrderByDisplayOrderAscIdAsc(25L))
        .thenReturn(List.of());
    when(statusHistoryRepository
        .findAllByStoredItem_IdOrderByIdAsc(25L))
        .thenReturn(List.of(history));

    service.delete(25L, 7L, false);

    verifyNoInteractions(fileResourceRepository, fileStorageRegistry);
    verify(statusHistoryRepository).deleteAll(List.of(history));
    verify(storedItemRepository).delete(item);
    assertThatCode(this::runAfterCommitCallbacks).doesNotThrowAnyException();
  }

  @Test
  void nonStoredItemCannotBeDeleted() {
    item = storedItem(StoredItemStatus.IN_PROGRESS);
    when(storedItemRepository.findDetailById(25L))
        .thenReturn(Optional.of(item));

    assertBusinessError(
        () -> service.delete(25L, 7L, false),
        StoredItemErrorCode.NOT_DELETABLE
    );
    verify(itemClaimRepository, never()).existsByStoredItem_Id(any());
    verify(storedItemRepository, never()).delete(item);
  }

  @Test
  void itemWithAnyClaimCannotBeDeleted() {
    when(itemClaimRepository.existsByStoredItem_Id(25L))
        .thenReturn(true);

    assertBusinessError(
        () -> service.delete(25L, 7L, false),
        StoredItemErrorCode.HAS_CLAIMS
    );
    verify(storedItemRepository, never()).delete(item);
  }

  @Test
  void staffWithoutOfficeAssignmentIsDenied() {
    when(assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(3L, 7L))
        .thenReturn(false);

    assertBusinessError(
        () -> service.delete(25L, 7L, false),
        StoredItemErrorCode.ACCESS_DENIED
    );
    verify(itemClaimRepository, never()).existsByStoredItem_Id(any());
  }

  @Test
  void adminCanDeleteWithoutOfficeAssignment() {
    when(attachmentRepository
        .findAllByStoredItem_IdOrderByDisplayOrderAscIdAsc(25L))
        .thenReturn(List.of());
    when(statusHistoryRepository
        .findAllByStoredItem_IdOrderByIdAsc(25L))
        .thenReturn(List.of());

    service.delete(25L, 7L, true);

    verify(assignmentRepository, never())
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(any(), any());
    verify(storedItemRepository).delete(item);
  }

  @Test
  void missingStoredItemReturnsNotFound() {
    when(storedItemRepository.findDetailById(99L))
        .thenReturn(Optional.empty());

    assertBusinessError(
        () -> service.delete(99L, 7L, false),
        StoredItemErrorCode.NOT_FOUND
    );
  }

  @Test
  void optimisticLockConflictReturnsDomainConflict() {
    when(attachmentRepository
        .findAllByStoredItem_IdOrderByDisplayOrderAscIdAsc(25L))
        .thenReturn(List.of());
    when(statusHistoryRepository
        .findAllByStoredItem_IdOrderByIdAsc(25L))
        .thenReturn(List.of());
    org.mockito.Mockito.doThrow(
        new OptimisticLockingFailureException("conflict")
    ).when(storedItemRepository).flush();

    assertBusinessError(
        () -> service.delete(25L, 7L, false),
        StoredItemErrorCode.VERSION_CONFLICT
    );
  }

  @Test
  void concurrentClaimForeignKeyConflictReturnsHasClaims() {
    when(attachmentRepository
        .findAllByStoredItem_IdOrderByDisplayOrderAscIdAsc(25L))
        .thenReturn(List.of());
    when(statusHistoryRepository
        .findAllByStoredItem_IdOrderByIdAsc(25L))
        .thenReturn(List.of());
    org.mockito.Mockito.doThrow(
        new DataIntegrityViolationException("claim inserted concurrently")
    ).when(storedItemRepository).flush();

    assertBusinessError(
        () -> service.delete(25L, 7L, false),
        StoredItemErrorCode.HAS_CLAIMS
    );
  }

  @Test
  void storageFailureAfterCommitDoesNotFailCompletedDatabaseDeletion() {
    StoredItemAttachment attachment = mock(StoredItemAttachment.class);
    FileResource file = file("S3", "2026/08/12/item.jpg");
    when(attachment.getFile()).thenReturn(file);
    when(attachmentRepository
        .findAllByStoredItem_IdOrderByDisplayOrderAscIdAsc(25L))
        .thenReturn(List.of(attachment));
    when(statusHistoryRepository
        .findAllByStoredItem_IdOrderByIdAsc(25L))
        .thenReturn(List.of());
    org.mockito.Mockito.doThrow(new FileStorageException(
        "storage unavailable",
        new IllegalStateException()
    )).when(fileStorage).delete("2026/08/12/item.jpg");

    service.delete(25L, 7L, false);

    assertThatCode(this::runAfterCommitCallbacks).doesNotThrowAnyException();
    verify(storedItemRepository).delete(item);
  }

  private StoredItem storedItem(StoredItemStatus status) {
    StoredItem result = mock(StoredItem.class);
    LostItemOffice office = mock(LostItemOffice.class);
    when(office.getId()).thenReturn(3L);
    when(result.getOffice()).thenReturn(office);
    when(result.getPublicStatus()).thenReturn(status);
    return result;
  }

  private FileResource file(String provider, String key) {
    FileResource file = mock(FileResource.class);
    when(file.getStorageProvider()).thenReturn(provider);
    when(file.getStorageKey()).thenReturn(key);
    return file;
  }

  private void runAfterCommitCallbacks() {
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(TransactionSynchronization::afterCommit);
  }

  private void assertBusinessError(
      Runnable action,
      StoredItemErrorCode expectedError
  ) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessException.class)
        .extracting(exception ->
            ((BusinessException) exception).getErrorCode()
        )
        .isEqualTo(expectedError);
  }
}
