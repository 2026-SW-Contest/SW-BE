package org.swbe.domain.lostitem.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ItemClaimStatusTest {

  @Test
  void exposesOnlySimplifiedStatuses() {
    assertThat(ItemClaimStatus.values())
        .containsExactly(
            ItemClaimStatus.WAITING,
            ItemClaimStatus.APPROVED,
            ItemClaimStatus.REJECTED
        );
  }

  @Test
  void activeStatusesExcludeTerminalStatuses() {
    assertThat(ItemClaimStatus.activeStatuses())
        .containsExactlyInAnyOrder(
            ItemClaimStatus.WAITING
        );
  }

  @Test
  void onlyApprovedAndRejectedAreDecisions() {
    assertThat(ItemClaimStatus.APPROVED.isDecision()).isTrue();
    assertThat(ItemClaimStatus.REJECTED.isDecision()).isTrue();
    assertThat(ItemClaimStatus.WAITING.isDecision()).isFalse();
  }
}
