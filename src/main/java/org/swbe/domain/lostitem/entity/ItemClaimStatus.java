package org.swbe.domain.lostitem.entity;

import java.util.Set;

public enum ItemClaimStatus {
  IN_PROGRESS,
  ADDITIONAL_INFO_REQUESTED,
  APPROVED,
  REJECTED,
  CANCELED,
  COLLECTED,
  CLOSED_BY_OTHER_COLLECTION,
  CLOSED_BY_STORAGE_END;

  private static final Set<ItemClaimStatus> ACTIVE_STATUSES = Set.of(
      IN_PROGRESS,
      ADDITIONAL_INFO_REQUESTED,
      APPROVED
  );

  public static Set<ItemClaimStatus> activeStatuses() {
    return ACTIVE_STATUSES;
  }
}
