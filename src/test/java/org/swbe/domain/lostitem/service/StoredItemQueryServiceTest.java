package org.swbe.domain.lostitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.lostitem.cursor.StoredItemCursor;
import org.swbe.domain.lostitem.cursor.StoredItemCursorCodec;
import org.swbe.domain.lostitem.dto.request.StoredItemSearchCondition;
import org.swbe.domain.lostitem.entity.ItemCategory;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemStatus;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.global.error.BusinessException;

class StoredItemQueryServiceTest {

  private StoredItemRepository storedItemRepository;
  private StoredItemCursorCodec cursorCodec;
  private StoredItemThumbnailService thumbnailService;
  private StoredItemQueryService service;

  @BeforeEach
  void setUp() {
    storedItemRepository = mock(StoredItemRepository.class);
    cursorCodec = mock(StoredItemCursorCodec.class);
    thumbnailService = mock(StoredItemThumbnailService.class);
    service = new StoredItemQueryService(
        storedItemRepository,
        cursorCodec,
        thumbnailService
    );
  }

  @Test
  void appliesAllFiltersAndDecodedCursor() {
    LocalDate from = LocalDate.of(2026, 7, 1);
    LocalDate to = LocalDate.of(2026, 8, 12);
    LocalDateTime cursorCreatedAt = LocalDateTime.of(
        2026,
        8,
        10,
        12,
        0
    );
    StoredItemSearchCondition condition = new StoredItemSearchCondition(
        3L,
        7L,
        StoredItemStatus.STORED,
        from,
        to,
        "cursor",
        20
    );
    when(cursorCodec.decode("cursor"))
        .thenReturn(new StoredItemCursor(cursorCreatedAt, 50L));
    when(storedItemRepository.findAllByCursor(
        3L,
        7L,
        StoredItemStatus.STORED,
        from,
        to,
        cursorCreatedAt,
        50L,
        21
    )).thenReturn(List.of());

    var response = service.getStoredItems(condition);

    assertThat(response.data().content()).isEmpty();
    assertThat(response.data().hasNext()).isFalse();
    assertThat(response.data().nextCursor()).isNull();
    verifyNoInteractions(thumbnailService);
  }

  @Test
  void createsNextCursorFromLastVisibleItem() {
    StoredItem first = item(30L, "검은색 지갑", 30, true);
    StoredItem second = item(20L, "학생증", 20, false);
    StoredItem extra = item(10L, "카드", 10, true);
    when(storedItemRepository.findAllByCursor(
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        eq(3)
    )).thenReturn(List.of(first, second, extra));
    when(thumbnailService.resolveAll(List.of(30L, 20L)))
        .thenReturn(Map.of(
            30L,
            "https://cdn.example.com/public/image.jpg"
        ));
    when(cursorCodec.encode(second.getCreatedAt(), 20L))
        .thenReturn("next-cursor");

    var response = service.getStoredItems(condition(2));

    assertThat(response.data().content()).hasSize(2);
    assertThat(response.data().content().getFirst().description())
        .isEqualTo("공개 설명");
    assertThat(response.data().content().getFirst().thumbnailUrl())
        .isEqualTo("https://cdn.example.com/public/image.jpg");
    assertThat(response.data().content().get(1).foundLocationName())
        .isNull();
    assertThat(response.data().content().get(1).thumbnailUrl()).isNull();
    assertThat(response.data().nextCursor()).isEqualTo("next-cursor");
    assertThat(response.data().hasNext()).isTrue();
    verify(cursorCodec).encode(second.getCreatedAt(), 20L);
  }

  @Test
  void sameCreatedAtCursorKeepsIdAsTieBreaker() {
    LocalDateTime sameCreatedAt = LocalDateTime.of(
        2026,
        8,
        12,
        12,
        0
    );
    when(cursorCodec.decode("same-time-cursor"))
        .thenReturn(new StoredItemCursor(sameCreatedAt, 20L));
    when(storedItemRepository.findAllByCursor(
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        eq(sameCreatedAt),
        eq(20L),
        eq(21)
    )).thenReturn(List.of());

    service.getStoredItems(new StoredItemSearchCondition(
        null,
        null,
        null,
        null,
        null,
        "same-time-cursor",
        20
    ));

    verify(storedItemRepository).findAllByCursor(
        null,
        null,
        null,
        null,
        null,
        sameCreatedAt,
        20L,
        21
    );
  }

  @Test
  void rejectsReversedFoundDateRange() {
    StoredItemSearchCondition condition = new StoredItemSearchCondition(
        null,
        null,
        null,
        LocalDate.of(2026, 8, 12),
        LocalDate.of(2026, 8, 1),
        null,
        20
    );

    assertThatThrownBy(() -> service.getStoredItems(condition))
        .isInstanceOf(BusinessException.class);
    verifyNoInteractions(storedItemRepository, cursorCodec);
  }

  private StoredItemSearchCondition condition(int size) {
    return new StoredItemSearchCondition(
        null,
        null,
        null,
        null,
        null,
        null,
        size
    );
  }

  private StoredItem item(
      Long id,
      String name,
      int minute,
      boolean withLocation
  ) {
    ItemCategory category = mock(ItemCategory.class);
    when(category.getName()).thenReturn("지갑/카드/현금");
    Location location = withLocation ? mock(Location.class) : null;
    if (location != null) {
      when(location.getName()).thenReturn("명진관 2층");
    }
    StoredItem item = mock(StoredItem.class);
    when(item.getId()).thenReturn(id);
    when(item.getItemName()).thenReturn(name);
    when(item.getPublicDescription()).thenReturn("공개 설명");
    when(item.getItemCategory()).thenReturn(category);
    when(item.getFoundLocation()).thenReturn(location);
    when(item.getFoundLocationName()).thenReturn(
        location == null ? null : "명진관 2층"
    );
    when(item.getFoundDate()).thenReturn(LocalDate.of(2026, 8, 10));
    when(item.getPublicStatus()).thenReturn(StoredItemStatus.STORED);
    when(item.getCreatedAt()).thenReturn(
        LocalDateTime.of(2026, 8, 10, 12, minute)
    );
    return item;
  }
}
