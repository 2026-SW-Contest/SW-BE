package org.swbe.domain.lostitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.lostitem.dto.request.StoredItemStatusUpdateRequest;
import org.swbe.domain.lostitem.entity.ItemCategory;
import org.swbe.domain.lostitem.entity.ItemStatusHistory;
import org.swbe.domain.lostitem.entity.LostItemOffice;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemStatus;
import org.swbe.domain.lostitem.repository.ItemStatusHistoryRepository;
import org.swbe.domain.lostitem.repository.OfficeStaffAssignmentRepository;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.global.error.BusinessException;

class StoredItemStatusUpdateServiceTest {

  private StoredItemRepository storedItemRepository;
  private ItemStatusHistoryRepository statusHistoryRepository;
  private OfficeStaffAssignmentRepository assignmentRepository;
  private AppUserRepository appUserRepository;
  private StoredItemStatusUpdateService service;
  private StoredItem item;
  private AppUser changer;

  @BeforeEach
  void setUp() {
    storedItemRepository = mock(StoredItemRepository.class);
    statusHistoryRepository = mock(ItemStatusHistoryRepository.class);
    assignmentRepository = mock(OfficeStaffAssignmentRepository.class);
    appUserRepository = mock(AppUserRepository.class);
    item = storedItem();
    changer = mock(AppUser.class);
    when(storedItemRepository.findDetailById(25L))
        .thenReturn(Optional.of(item));
    when(assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(3L, 7L))
        .thenReturn(true);
    when(appUserRepository.findById(7L))
        .thenReturn(Optional.of(changer));
    service = new StoredItemStatusUpdateService(
        storedItemRepository,
        statusHistoryRepository,
        assignmentRepository,
        appUserRepository,
        Clock.fixed(
            Instant.parse("2026-08-12T07:30:00Z"),
            ZoneOffset.UTC
        )
    );
  }

  @Test
  void changesStoredToProgressAndRecordsHistory() {
    var response = service.updateStatus(
        25L,
        new StoredItemStatusUpdateRequest(
            StoredItemStatus.IN_PROGRESS,
            "  소유자 확인 요청 접수  "
        ),
        7L,
        false
    );

    assertThat(item.getPublicStatus())
        .isEqualTo(StoredItemStatus.IN_PROGRESS);
    assertThat(response.data().previousStatus()).isEqualTo("STORED");
    assertThat(response.data().publicStatus()).isEqualTo("IN_PROGRESS");
    assertThat(response.data().publicStatusName()).isEqualTo("진행중");
    assertThat(response.data().changed()).isTrue();
    assertThat(response.data().changedAt()).isEqualTo(
        LocalDateTime.of(2026, 8, 12, 7, 30)
    );
    ArgumentCaptor<ItemStatusHistory> history = ArgumentCaptor.forClass(
        ItemStatusHistory.class
    );
    verify(statusHistoryRepository).save(history.capture());
    assertThat(history.getValue().getStoredItem()).isSameAs(item);
    assertThat(history.getValue().getChangedBy()).isSameAs(changer);
    assertThat(history.getValue().getActorType()).isEqualTo("USER");
    assertThat(history.getValue().getPreviousStatus())
        .isEqualTo(StoredItemStatus.STORED);
    assertThat(history.getValue().getNewStatus())
        .isEqualTo(StoredItemStatus.IN_PROGRESS);
    assertThat(history.getValue().getChangeReason())
        .isEqualTo("소유자 확인 요청 접수");
  }

  @Test
  void allowsDirectStoredToCompletedWithoutSettingLegacyCloseFields() {
    var response = service.updateStatus(
        25L,
        new StoredItemStatusUpdateRequest(
            StoredItemStatus.COMPLETED,
            null
        ),
        7L,
        false
    );

    assertThat(response.data().publicStatus()).isEqualTo("COMPLETED");
    assertThat(item.getCollectedAt()).isNull();
    assertThat(item.getStorageClosedAt()).isNull();
    assertThat(item.getStorageCloseReason()).isNull();
  }

  @Test
  void sameStatusIsIdempotentAndDoesNotRecordHistory() {
    var response = service.updateStatus(
        25L,
        new StoredItemStatusUpdateRequest(StoredItemStatus.STORED, null),
        7L,
        false
    );

    assertThat(response.data().changed()).isFalse();
    assertThat(response.data().changedAt()).isNull();
    verify(statusHistoryRepository, never()).save(any());
    verify(appUserRepository, never()).findById(any());
    verify(storedItemRepository, never()).flush();
  }

  @Test
  void rejectsBackwardTransition() {
    item.changeStatus(
        StoredItemStatus.IN_PROGRESS,
        LocalDateTime.of(2026, 8, 11, 12, 0)
    );

    assertThatThrownBy(() -> service.updateStatus(
        25L,
        new StoredItemStatusUpdateRequest(StoredItemStatus.STORED, null),
        7L,
        false
    )).isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode().code()
        ).isEqualTo("STORED_ITEM_INVALID_STATUS_TRANSITION"));
    verify(statusHistoryRepository, never()).save(any());
  }

  @Test
  void completedStatusIsTerminal() {
    item.changeStatus(
        StoredItemStatus.COMPLETED,
        LocalDateTime.of(2026, 8, 11, 12, 0)
    );

    assertThatThrownBy(() -> service.updateStatus(
        25L,
        new StoredItemStatusUpdateRequest(
            StoredItemStatus.IN_PROGRESS,
            null
        ),
        7L,
        false
    )).isInstanceOf(BusinessException.class);
  }

  @Test
  void staffWithoutCurrentOfficeAssignmentIsDenied() {
    when(assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(3L, 7L))
        .thenReturn(false);

    assertThatThrownBy(() -> service.updateStatus(
        25L,
        new StoredItemStatusUpdateRequest(
            StoredItemStatus.IN_PROGRESS,
            null
        ),
        7L,
        false
    )).isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode().code()
        ).isEqualTo("STORED_ITEM_ACCESS_DENIED"));
  }

  @Test
  void adminCanChangeStatusWithoutOfficeAssignment() {
    var response = service.updateStatus(
        25L,
        new StoredItemStatusUpdateRequest(
            StoredItemStatus.IN_PROGRESS,
            null
        ),
        7L,
        true
    );

    assertThat(response.data().changed()).isTrue();
    verify(assignmentRepository, never())
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(any(), any());
  }

  @Test
  void optimisticLockConflictReturnsDomainConflict() {
    org.mockito.Mockito.doThrow(
        new OptimisticLockingFailureException("conflict")
    ).when(storedItemRepository).flush();

    assertThatThrownBy(() -> service.updateStatus(
        25L,
        new StoredItemStatusUpdateRequest(
            StoredItemStatus.IN_PROGRESS,
            null
        ),
        7L,
        false
    )).isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode().code()
        ).isEqualTo("STORED_ITEM_VERSION_CONFLICT"));
  }

  private StoredItem storedItem() {
    LostItemOffice office = mock(LostItemOffice.class);
    when(office.getId()).thenReturn(3L);
    StoredItem result = StoredItem.create(
        office,
        mock(Location.class),
        null,
        mock(AppUser.class),
        mock(ItemCategory.class),
        "검은색 지갑",
        "공개 설명",
        null,
        LocalDate.of(2026, 8, 10),
        LocalDateTime.of(2026, 8, 10, 14, 30)
    );
    ReflectionTestUtils.setField(result, "id", 25L);
    return result;
  }
}
