package org.swbe.domain.lostitem.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StoredItemTest {

  @Test
  void newStoredItemHasStoredStatusByDefault() {
    StoredItem storedItem = new StoredItem();

    assertThat(storedItem.getPublicStatus())
        .isEqualTo(StoredItemStatus.STORED);
  }
}
