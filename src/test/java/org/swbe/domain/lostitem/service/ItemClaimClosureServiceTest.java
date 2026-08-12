package org.swbe.domain.lostitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.swbe.domain.lostitem.entity.ClaimStatusHistory;
import org.swbe.domain.lostitem.entity.ItemClaim;
import org.swbe.domain.lostitem.entity.ItemClaimStatus;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.repository.ClaimStatusHistoryRepository;
import org.swbe.domain.lostitem.repository.ItemClaimRepository;
import org.swbe.domain.notification.entity.Notification;
import org.swbe.domain.notification.repository.NotificationRepository;
import org.swbe.domain.user.entity.AppUser;

class ItemClaimClosureServiceTest {

  @Test
  void rejectsAllWaitingClaimsExceptApprovedClaim() {
    ItemClaimRepository claimRepository = mock(ItemClaimRepository.class);
    ClaimStatusHistoryRepository historyRepository =
        mock(ClaimStatusHistoryRepository.class);
    NotificationRepository notificationRepository =
        mock(NotificationRepository.class);
    ItemClaimClosureService service = new ItemClaimClosureService(
        claimRepository,
        historyRepository,
        notificationRepository
    );
    StoredItem item = mock(StoredItem.class);
    when(item.getId()).thenReturn(25L);
    AppUser reviewer = mock(AppUser.class);
    ItemClaim excluded = claim(item, mock(AppUser.class), 31L);
    AppUser otherClaimant = mock(AppUser.class);
    ItemClaim other = claim(item, otherClaimant, 32L);
    when(claimRepository.findAllByStoredItemIdAndStatus(
        25L,
        ItemClaimStatus.WAITING
    )).thenReturn(List.of(excluded, other));
    LocalDateTime now = LocalDateTime.of(2026, 8, 12, 7, 30);

    int rejectedCount = service.rejectWaitingClaims(
        item,
        31L,
        reviewer,
        now
    );

    assertThat(rejectedCount).isEqualTo(1);
    assertThat(excluded.getClaimStatus()).isEqualTo(ItemClaimStatus.WAITING);
    assertThat(other.getClaimStatus()).isEqualTo(ItemClaimStatus.REJECTED);
    assertThat(other.getDecisionMessage()).isEqualTo(
        ItemClaimClosureService.OTHER_COLLECTION_MESSAGE
    );
    ArgumentCaptor<ClaimStatusHistory> history = ArgumentCaptor.forClass(
        ClaimStatusHistory.class
    );
    verify(historyRepository).save(history.capture());
    assertThat(history.getValue().getActorType()).isEqualTo("SYSTEM");
    assertThat(history.getValue().getChangedBy()).isNull();
    verify(notificationRepository).save(any(Notification.class));
  }

  private ItemClaim claim(
      StoredItem item,
      AppUser claimant,
      Long id
  ) {
    ItemClaim claim = ItemClaim.createOnline(
        item,
        claimant,
        "소유 증명",
        LocalDateTime.of(2026, 8, 12, 6, 0)
    );
    ReflectionTestUtils.setField(claim, "id", id);
    return claim;
  }
}
