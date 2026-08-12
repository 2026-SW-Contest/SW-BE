package org.swbe.domain.lostitem.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ItemClaimStatusTest {

  @Test
  void exposesOnlySimplifiedStatuses() {
    assertThat(ItemClaimStatus.values())
        .containsExactly(
            ItemClaimStatus.WAITING,
            ItemClaimStatus.IN_PROGRESS,
            ItemClaimStatus.APPROVED,
            ItemClaimStatus.REJECTED,
            ItemClaimStatus.CLOSED_BY_OTHER_COLLECTION
        );
  }

  @Test
  void activeStatusesExcludeTerminalStatuses() {
    assertThat(ItemClaimStatus.activeStatuses())
        .containsExactlyInAnyOrder(
            ItemClaimStatus.WAITING,
            ItemClaimStatus.IN_PROGRESS,
            ItemClaimStatus.APPROVED
        );
  }
}
