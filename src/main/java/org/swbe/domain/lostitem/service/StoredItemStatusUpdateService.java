package org.swbe.domain.lostitem.service;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.lostitem.dto.request.StoredItemStatusUpdateRequest;
import org.swbe.domain.lostitem.dto.response.StoredItemStatusUpdateDataResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemStatusUpdateResponse;
import org.swbe.domain.lostitem.entity.ItemStatusHistory;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemStatus;
import org.swbe.domain.lostitem.exception.StoredItemErrorCode;
import org.swbe.domain.lostitem.repository.ItemStatusHistoryRepository;
import org.swbe.domain.lostitem.repository.OfficeStaffAssignmentRepository;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.exception.AuthErrorCode;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.global.error.BusinessException;

@Service
@RequiredArgsConstructor
public class StoredItemStatusUpdateService {

  private final StoredItemRepository storedItemRepository;
  private final ItemStatusHistoryRepository statusHistoryRepository;
  private final OfficeStaffAssignmentRepository assignmentRepository;
  private final AppUserRepository appUserRepository;
  private final Clock clock;

  @Transactional
  public StoredItemStatusUpdateResponse updateStatus(
      Long storedItemId,
      StoredItemStatusUpdateRequest request,
      Long changerUserId,
      boolean admin
  ) {
    StoredItem item = storedItemRepository
        .findDetailById(storedItemId)
        .orElseThrow(() -> new BusinessException(
            StoredItemErrorCode.NOT_FOUND
        ));
    validateOfficeAccess(item, changerUserId, admin);

    StoredItemStatus previousStatus = item.getPublicStatus();
    StoredItemStatus newStatus = request.status();
    if (previousStatus == newStatus) {
      return response(item, previousStatus, false, null);
    }
    if (!previousStatus.canTransitionTo(newStatus)) {
      throw new BusinessException(
          StoredItemErrorCode.INVALID_STATUS_TRANSITION
      );
    }

    AppUser changer = appUserRepository.findById(changerUserId)
        .orElseThrow(() -> new BusinessException(
            AuthErrorCode.ACCOUNT_NOT_FOUND
        ));
    LocalDateTime now = LocalDateTime.now(clock);
    try {
      item.changeStatus(newStatus, now);
      statusHistoryRepository.save(ItemStatusHistory.recordTransition(
          item,
          changer,
          previousStatus,
          newStatus,
          request.changeReason(),
          now
      ));
      storedItemRepository.flush();
      return response(item, previousStatus, true, now);
    } catch (OptimisticLockingFailureException exception) {
      throw new BusinessException(StoredItemErrorCode.VERSION_CONFLICT);
    }
  }

  private void validateOfficeAccess(
      StoredItem item,
      Long userId,
      boolean admin
  ) {
    if (!admin && !assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(
            item.getOffice().getId(),
            userId
        )) {
      throw new BusinessException(StoredItemErrorCode.ACCESS_DENIED);
    }
  }

  private StoredItemStatusUpdateResponse response(
      StoredItem item,
      StoredItemStatus previousStatus,
      boolean changed,
      LocalDateTime changedAt
  ) {
    return new StoredItemStatusUpdateResponse(
        new StoredItemStatusUpdateDataResponse(
            item.getId(),
            previousStatus.name(),
            item.getPublicStatus().name(),
            item.getPublicStatus().getDisplayName(),
            changed,
            changedAt
        )
    );
  }
}
