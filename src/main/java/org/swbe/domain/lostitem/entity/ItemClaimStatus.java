package org.swbe.domain.lostitem.entity;

import java.util.Set;

public enum ItemClaimStatus {
  WAITING,
  IN_PROGRESS,
  APPROVED,
  REJECTED,
  CLOSED_BY_OTHER_COLLECTION;

  private static final Set<ItemClaimStatus> ACTIVE_STATUSES = Set.of(
      WAITING,
      IN_PROGRESS,
      APPROVED
  );

  public static Set<ItemClaimStatus> activeStatuses() {
    return ACTIVE_STATUSES;
  }
}
