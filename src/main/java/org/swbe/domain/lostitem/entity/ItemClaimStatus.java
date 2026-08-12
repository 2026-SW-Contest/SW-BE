package org.swbe.domain.lostitem.entity;

import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemClaimStatus {
  WAITING("대기"),
  APPROVED("승인"),
  REJECTED("거부");

  private static final Set<ItemClaimStatus> ACTIVE_STATUSES = Set.of(
      WAITING
  );

  private final String displayName;

  public static Set<ItemClaimStatus> activeStatuses() {
    return ACTIVE_STATUSES;
  }

  public boolean isDecision() {
    return this == APPROVED || this == REJECTED;
  }
}
