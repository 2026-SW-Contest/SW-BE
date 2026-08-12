package org.swbe.domain.lostitem.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.swbe.domain.lostitem.entity.ClaimStatusHistory;
import org.swbe.domain.lostitem.entity.ItemClaim;
import org.swbe.domain.lostitem.entity.ItemClaimStatus;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.repository.ClaimStatusHistoryRepository;
import org.swbe.domain.lostitem.repository.ItemClaimRepository;
import org.swbe.domain.notification.entity.Notification;
import org.swbe.domain.notification.repository.NotificationRepository;
import org.swbe.domain.user.entity.AppUser;

@Component
@RequiredArgsConstructor
public class ItemClaimClosureService {

  public static final String OTHER_COLLECTION_MESSAGE =
      "물품이 다른 소유자에게 인계되어 요청이 종료되었습니다.";

  private final ItemClaimRepository itemClaimRepository;
  private final ClaimStatusHistoryRepository historyRepository;
  private final NotificationRepository notificationRepository;

  public int rejectWaitingClaims(
      StoredItem storedItem,
      Long excludedClaimId,
      AppUser reviewer,
      LocalDateTime now
  ) {
    List<ItemClaim> waitingClaims = itemClaimRepository
        .findAllByStoredItemIdAndStatus(
            storedItem.getId(),
            ItemClaimStatus.WAITING
        );
    int rejectedCount = 0;
    for (ItemClaim claim : waitingClaims) {
      if (claim.getId().equals(excludedClaimId)) {
        continue;
      }
      claim.decide(
          ItemClaimStatus.REJECTED,
          reviewer,
          OTHER_COLLECTION_MESSAGE,
          now
      );
      historyRepository.save(
          ClaimStatusHistory.recordSystemTransition(
              claim,
              ItemClaimStatus.WAITING,
              ItemClaimStatus.REJECTED,
              OTHER_COLLECTION_MESSAGE,
              now
          )
      );
      notifyClaimant(claim, storedItem, now);
      rejectedCount++;
    }
    return rejectedCount;
  }

  private void notifyClaimant(
      ItemClaim claim,
      StoredItem storedItem,
      LocalDateTime now
  ) {
    if (claim.getClaimantUser() == null) {
      return;
    }
    notificationRepository.save(
        Notification.createItemClaimDecision(
            claim.getClaimantUser(),
            storedItem.getId(),
            "소유자 확인 요청이 종료되었습니다.",
            OTHER_COLLECTION_MESSAGE,
            now
        )
    );
  }
}
