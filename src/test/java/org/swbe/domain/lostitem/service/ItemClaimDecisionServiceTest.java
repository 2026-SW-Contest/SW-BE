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
import org.springframework.test.util.ReflectionTestUtils;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.lostitem.dto.request.ItemClaimDecisionRequest;
import org.swbe.domain.lostitem.entity.ClaimStatusHistory;
import org.swbe.domain.lostitem.entity.ItemCategory;
import org.swbe.domain.lostitem.entity.ItemClaim;
import org.swbe.domain.lostitem.entity.ItemClaimStatus;
import org.swbe.domain.lostitem.entity.ItemStatusHistory;
import org.swbe.domain.lostitem.entity.LostItemOffice;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemStatus;
import org.swbe.domain.lostitem.exception.ItemClaimErrorCode;
import org.swbe.domain.lostitem.repository.ClaimStatusHistoryRepository;
import org.swbe.domain.lostitem.repository.ItemClaimRepository;
import org.swbe.domain.lostitem.repository.ItemStatusHistoryRepository;
import org.swbe.domain.lostitem.repository.OfficeStaffAssignmentRepository;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.domain.notification.entity.Notification;
import org.swbe.domain.notification.repository.NotificationRepository;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.global.error.BusinessException;

class ItemClaimDecisionServiceTest {

  private static final LocalDateTime NOW =
      LocalDateTime.of(2026, 8, 12, 7, 30);

  private ItemClaimRepository itemClaimRepository;
  private ClaimStatusHistoryRepository claimHistoryRepository;
  private ItemStatusHistoryRepository itemHistoryRepository;
  private StoredItemRepository storedItemRepository;
  private OfficeStaffAssignmentRepository assignmentRepository;
  private AppUserRepository appUserRepository;
  private NotificationRepository notificationRepository;
  private ItemClaimClosureService closureService;
  private ItemClaimDecisionService service;
  private StoredItem storedItem;
  private ItemClaim claim;
  private AppUser claimant;
  private AppUser reviewer;

  @BeforeEach
  void setUp() {
    itemClaimRepository = mock(ItemClaimRepository.class);
    claimHistoryRepository = mock(ClaimStatusHistoryRepository.class);
    itemHistoryRepository = mock(ItemStatusHistoryRepository.class);
    storedItemRepository = mock(StoredItemRepository.class);
    assignmentRepository = mock(OfficeStaffAssignmentRepository.class);
    appUserRepository = mock(AppUserRepository.class);
    notificationRepository = mock(NotificationRepository.class);
    closureService = mock(ItemClaimClosureService.class);
    claimant = mock(AppUser.class);
    reviewer = mock(AppUser.class);
    storedItem = storedItem();
    claim = ItemClaim.createOnline(
        storedItem,
        claimant,
        "지갑 내부의 카드 정보",
        LocalDateTime.of(2026, 8, 12, 6, 0)
    );
    ReflectionTestUtils.setField(claim, "id", 31L);

    when(itemClaimRepository.findDetailById(31L))
        .thenReturn(Optional.of(claim));
    when(assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(3L, 7L))
        .thenReturn(true);
    when(appUserRepository.findById(7L))
        .thenReturn(Optional.of(reviewer));
    service = new ItemClaimDecisionService(
        itemClaimRepository,
        claimHistoryRepository,
        itemHistoryRepository,
        storedItemRepository,
        assignmentRepository,
        appUserRepository,
        notificationRepository,
        closureService,
        Clock.fixed(
            Instant.parse("2026-08-12T07:30:00Z"),
            ZoneOffset.UTC
        )
    );
  }

  @Test
  void approvalMessageIsOptionalAndCompletesStoredItem() {
    var response = service.decide(
        31L,
        new ItemClaimDecisionRequest(ItemClaimStatus.APPROVED, null),
        7L,
        false
    );

    assertThat(claim.getClaimStatus()).isEqualTo(ItemClaimStatus.APPROVED);
    assertThat(claim.getDecisionMessage()).isNull();
    assertThat(claim.getReviewedBy()).isSameAs(reviewer);
    assertThat(claim.getDecidedAt()).isEqualTo(NOW);
    assertThat(storedItem.getPublicStatus())
        .isEqualTo(StoredItemStatus.COMPLETED);
    assertThat(response.data().decision()).isEqualTo("APPROVED");
    assertThat(response.data().message()).isNull();
    verify(closureService).rejectWaitingClaims(
        storedItem,
        31L,
        reviewer,
        NOW
    );
    verify(itemHistoryRepository).save(any(ItemStatusHistory.class));
    verify(notificationRepository).save(any(Notification.class));
  }

  @Test
  void rejectionStoresRequiredMessageWithoutCompletingItem() {
    var response = service.decide(
        31L,
        new ItemClaimDecisionRequest(
            ItemClaimStatus.REJECTED,
            "  제출한 정보가 부족합니다.  "
        ),
        7L,
        false
    );

    assertThat(claim.getClaimStatus()).isEqualTo(ItemClaimStatus.REJECTED);
    assertThat(claim.getDecisionMessage())
        .isEqualTo("제출한 정보가 부족합니다.");
    assertThat(storedItem.getPublicStatus())
        .isEqualTo(StoredItemStatus.STORED);
    assertThat(response.data().decisionName()).isEqualTo("거부");
    verify(itemHistoryRepository, never()).save(any());
    verify(closureService, never()).rejectWaitingClaims(
        any(),
        any(),
        any(),
        any()
    );
  }

  @Test
  void rejectionWithoutMessageIsRejected() {
    assertBusinessError(
        () -> service.decide(
            31L,
            new ItemClaimDecisionRequest(ItemClaimStatus.REJECTED, " "),
            7L,
            false
        ),
        ItemClaimErrorCode.DECISION_MESSAGE_REQUIRED
    );
    verify(itemClaimRepository, never()).findDetailById(any());
  }

  @Test
  void waitingCannotBeUsedAsDecision() {
    assertBusinessError(
        () -> service.decide(
            31L,
            new ItemClaimDecisionRequest(ItemClaimStatus.WAITING, null),
            7L,
            false
        ),
        ItemClaimErrorCode.INVALID_DECISION
    );
  }

  @Test
  void decidedClaimCannotBeDecidedAgain() {
    claim.decide(
        ItemClaimStatus.REJECTED,
        reviewer,
        "거부 사유",
        NOW.minusHours(1)
    );

    assertBusinessError(
        () -> service.decide(
            31L,
            new ItemClaimDecisionRequest(ItemClaimStatus.APPROVED, null),
            7L,
            false
        ),
        ItemClaimErrorCode.ALREADY_DECIDED
    );
  }

  @Test
  void staffWithoutOfficeAssignmentIsDenied() {
    when(assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(3L, 7L))
        .thenReturn(false);

    assertBusinessError(
        () -> service.decide(
            31L,
            new ItemClaimDecisionRequest(ItemClaimStatus.APPROVED, null),
            7L,
            false
        ),
        ItemClaimErrorCode.ACCESS_DENIED
    );
  }

  @Test
  void adminCanDecideWithoutOfficeAssignment() {
    service.decide(
        31L,
        new ItemClaimDecisionRequest(ItemClaimStatus.REJECTED, "거부"),
        7L,
        true
    );

    verify(assignmentRepository, never())
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(any(), any());
  }

  private StoredItem storedItem() {
    LostItemOffice office = mock(LostItemOffice.class);
    when(office.getId()).thenReturn(3L);
    StoredItem item = StoredItem.create(
        office,
        mock(Location.class),
        null,
        mock(AppUser.class),
        mock(ItemCategory.class),
        "갈색 지갑",
        "공개 설명",
        null,
        LocalDate.of(2026, 8, 10),
        LocalDateTime.of(2026, 8, 10, 14, 30)
    );
    ReflectionTestUtils.setField(item, "id", 25L);
    return item;
  }

  private void assertBusinessError(
      Runnable action,
      ItemClaimErrorCode expected
  ) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(expected)
        );
  }
}
