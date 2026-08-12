package org.swbe.domain.lostitem.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.user.entity.AppUser;

class StoredItemTest {

  @Test
  void newStoredItemHasStoredStatusByDefault() {
    StoredItem storedItem = new StoredItem();

    assertThat(storedItem.getPublicStatus())
        .isEqualTo(StoredItemStatus.STORED);
  }

  @Test
  void updateCanSwitchToFreeTextLocationAndClearPrivateDescription() {
    StoredItem item = StoredItem.create(
        mock(LostItemOffice.class),
        mock(Location.class),
        null,
        mock(AppUser.class),
        mock(ItemCategory.class),
        "지갑",
        "공개 설명",
        "내부 설명",
        LocalDate.of(2026, 8, 10),
        LocalDateTime.of(2026, 8, 10, 12, 0)
    );

    item.update(
        null,
        null,
        null,
        " 명진관 앞 벤치 ",
        true,
        " 수정된 지갑 ",
        null,
        null,
        true,
        null,
        LocalDateTime.of(2026, 8, 12, 15, 0)
    );

    assertThat(item.getFoundLocation()).isNull();
    assertThat(item.getFoundLocationText()).isEqualTo("명진관 앞 벤치");
    assertThat(item.getItemName()).isEqualTo("수정된 지갑");
    assertThat(item.getPrivateDescription()).isNull();
  }

  @Test
  void updateRejectsMissingLocationValuesWhenLocationChanges() {
    StoredItem item = StoredItem.create(
        mock(LostItemOffice.class),
        mock(Location.class),
        null,
        mock(AppUser.class),
        mock(ItemCategory.class),
        "지갑",
        "공개 설명",
        null,
        LocalDate.of(2026, 8, 10),
        LocalDateTime.of(2026, 8, 10, 12, 0)
    );

    assertThatThrownBy(() -> item.update(
        null,
        null,
        null,
        null,
        true,
        null,
        null,
        null,
        false,
        null,
        LocalDateTime.of(2026, 8, 12, 15, 0)
    )).isInstanceOf(IllegalArgumentException.class);
  }
}
