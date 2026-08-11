package org.swbe.domain.lostitem.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StoredItemStatusTest {

  @Test
  void providesThreePublicStatusesAndDisplayNames() {
    assertThat(StoredItemStatus.values()).containsExactly(
        StoredItemStatus.STORED,
        StoredItemStatus.IN_PROGRESS,
        StoredItemStatus.COMPLETED
    );
    assertThat(StoredItemStatus.STORED.getDisplayName())
        .isEqualTo("보관중");
    assertThat(StoredItemStatus.IN_PROGRESS.getDisplayName())
        .isEqualTo("진행중");
    assertThat(StoredItemStatus.COMPLETED.getDisplayName())
        .isEqualTo("해결완료");
  }
}
