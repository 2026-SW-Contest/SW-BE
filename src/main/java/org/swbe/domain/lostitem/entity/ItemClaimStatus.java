package org.swbe.domain.lostitem.entity;

import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemClaimStatus {
  WAITING("대기"),
  IN_PROGRESS("진행중"),
  APPROVED("승인"),
  REJECTED("반려"),
  CLOSED_BY_OTHER_COLLECTION("다른 소유자 인계로 종료");

  private static final Set<ItemClaimStatus> ACTIVE_STATUSES = Set.of(
      WAITING,
      IN_PROGRESS,
      APPROVED
  );

  private final String displayName;

  public static Set<ItemClaimStatus> activeStatuses() {
    return ACTIVE_STATUSES;
  }
}
