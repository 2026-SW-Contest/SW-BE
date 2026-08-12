package org.swbe.domain.lostitem.service;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.lostitem.dto.request.ItemClaimDecisionRequest;
import org.swbe.domain.lostitem.dto.response.ItemClaimDecisionDataResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimDecisionResponse;
import org.swbe.domain.lostitem.entity.ClaimStatusHistory;
import org.swbe.domain.lostitem.entity.ItemClaim;
import org.swbe.domain.lostitem.entity.ItemClaimStatus;
import org.swbe.domain.lostitem.entity.ItemStatusHistory;
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
import org.swbe.domain.user.exception.AuthErrorCode;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.global.error.BusinessException;

@Service
@RequiredArgsConstructor
public class ItemClaimDecisionService {

  private static final String DEFAULT_APPROVAL_MESSAGE =
      "소유자 확인 요청이 승인되었습니다.";

  private final ItemClaimRepository itemClaimRepository;
  private final ClaimStatusHistoryRepository claimHistoryRepository;
  private final ItemStatusHistoryRepository itemHistoryRepository;
  private final StoredItemRepository storedItemRepository;
  private final OfficeStaffAssignmentRepository assignmentRepository;
  private final AppUserRepository appUserRepository;
  private final NotificationRepository notificationRepository;
  private final ItemClaimClosureService closureService;
  private final Clock clock;

  @Transactional
  public ItemClaimDecisionResponse decide(
      Long itemClaimId,
      ItemClaimDecisionRequest request,
      Long reviewerUserId,
      boolean admin
  ) {
    validateDecisionRequest(request);
    ItemClaim claim = itemClaimRepository.findDetailById(itemClaimId)
        .orElseThrow(() -> new BusinessException(
            ItemClaimErrorCode.NOT_FOUND
        ));
    StoredItem storedItem = claim.getStoredItem();
    validateOfficeAccess(storedItem, reviewerUserId, admin);
    if (claim.getClaimStatus() != ItemClaimStatus.WAITING) {
      throw new BusinessException(ItemClaimErrorCode.ALREADY_DECIDED);
    }
    if (storedItem.getPublicStatus() == StoredItemStatus.COMPLETED) {
      throw new BusinessException(ItemClaimErrorCode.NOT_CLAIMABLE);
    }

    AppUser reviewer = appUserRepository.findById(reviewerUserId)
        .orElseThrow(() -> new BusinessException(
            AuthErrorCode.ACCOUNT_NOT_FOUND
        ));
    LocalDateTime now = LocalDateTime.now(clock);
    ItemClaimStatus decision = request.decision();

    try {
      claim.decide(decision, reviewer, request.message(), now);
      claimHistoryRepository.save(ClaimStatusHistory.recordTransition(
          claim,
          reviewer,
          ItemClaimStatus.WAITING,
          decision,
          request.message(),
          now
      ));
      if (decision == ItemClaimStatus.APPROVED) {
        completeStoredItem(storedItem, claim.getId(), reviewer, now);
      }
      notifyClaimant(claim, storedItem, decision, request.message(), now);
      itemClaimRepository.flush();
      storedItemRepository.flush();
    } catch (OptimisticLockingFailureException exception) {
      throw new BusinessException(ItemClaimErrorCode.VERSION_CONFLICT);
    }

    return new ItemClaimDecisionResponse(
        new ItemClaimDecisionDataResponse(
            claim.getId(),
            storedItem.getId(),
            decision.name(),
            decision.getDisplayName(),
            claim.getDecisionMessage(),
            claim.getDecidedAt()
        )
    );
  }

  private void validateDecisionRequest(ItemClaimDecisionRequest request) {
    if (!request.decision().isDecision()) {
      throw new BusinessException(ItemClaimErrorCode.INVALID_DECISION);
    }
    if (request.decision() == ItemClaimStatus.REJECTED
        && request.message() == null) {
      throw new BusinessException(
          ItemClaimErrorCode.DECISION_MESSAGE_REQUIRED
      );
    }
  }

  private void validateOfficeAccess(
      StoredItem storedItem,
      Long reviewerUserId,
      boolean admin
  ) {
    if (!admin && !assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(
            storedItem.getOffice().getId(),
            reviewerUserId
        )) {
      throw new BusinessException(ItemClaimErrorCode.ACCESS_DENIED);
    }
  }

  private void completeStoredItem(
      StoredItem storedItem,
      Long approvedClaimId,
      AppUser reviewer,
      LocalDateTime now
  ) {
    StoredItemStatus previousStatus = storedItem.getPublicStatus();
    storedItem.changeStatus(StoredItemStatus.COMPLETED, now);
    itemHistoryRepository.save(ItemStatusHistory.recordTransition(
        storedItem,
        reviewer,
        previousStatus,
        StoredItemStatus.COMPLETED,
        "소유자 확인 요청 승인",
        now
    ));
    closureService.rejectWaitingClaims(
        storedItem,
        approvedClaimId,
        reviewer,
        now
    );
  }

  private void notifyClaimant(
      ItemClaim claim,
      StoredItem storedItem,
      ItemClaimStatus decision,
      String message,
      LocalDateTime now
  ) {
    if (claim.getClaimantUser() == null) {
      return;
    }
    boolean approved = decision == ItemClaimStatus.APPROVED;
    String title = approved
        ? "소유자 확인 요청이 승인되었습니다."
        : "소유자 확인 요청이 거부되었습니다.";
    String content = message == null
        ? DEFAULT_APPROVAL_MESSAGE
        : message;
    notificationRepository.save(
        Notification.createItemClaimDecision(
            claim.getClaimantUser(),
            storedItem.getId(),
            title,
            content,
            now
        )
    );
  }
}
