package org.swbe.domain.lostitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.swbe.domain.lostitem.cursor.ItemClaimCursorCodec;
import org.swbe.domain.lostitem.dto.response.MyItemClaimListResponse;
import org.swbe.domain.lostitem.entity.ItemCategory;
import org.swbe.domain.lostitem.entity.ItemClaim;
import org.swbe.domain.lostitem.entity.ItemClaimStatus;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.repository.ItemClaimRepository;

class MyItemClaimQueryServiceTest {

  private ItemClaimRepository itemClaimRepository;
  private StoredItemThumbnailService thumbnailService;
  private ItemClaimCursorCodec cursorCodec;
  private MyItemClaimQueryService service;

  @BeforeEach
  void setUp() {
    itemClaimRepository = mock(ItemClaimRepository.class);
    thumbnailService = mock(StoredItemThumbnailService.class);
    cursorCodec = new ItemClaimCursorCodec();
    service = new MyItemClaimQueryService(
        itemClaimRepository,
        thumbnailService,
        cursorCodec
    );
  }

  @Test
  void returnsCurrentUsersClaimsWithStoredItemThumbnailAndCursor() {
    ItemClaim first = claim(
        37L,
        15L,
        ItemClaimStatus.WAITING,
        LocalDateTime.of(2026, 8, 12, 14, 30)
    );
    ItemClaim second = claim(
        31L,
        9L,
        ItemClaimStatus.APPROVED,
        LocalDateTime.of(2026, 8, 7, 10, 20)
    );
    when(itemClaimRepository.findAllByClaimantUserIdAndCursor(
        eq(7L),
        eq(null),
        eq(null),
        any(Pageable.class)
    )).thenReturn(List.of(first, second));
    when(thumbnailService.resolveAll(List.of(15L))).thenReturn(
        Map.of(15L, "https://cdn.example.com/stored-item-15.jpg")
    );

    MyItemClaimListResponse response = service.getMyItemClaims(
        7L,
        null,
        1
    );

    assertThat(response.data().content()).singleElement()
        .satisfies(item -> {
          assertThat(item.itemClaimId()).isEqualTo(37L);
          assertThat(item.storedItemId()).isEqualTo(15L);
          assertThat(item.itemName()).isEqualTo("Black wallet");
          assertThat(item.categoryName()).isEqualTo("Wallet");
          assertThat(item.foundLocationName()).isEqualTo("Student Center");
          assertThat(item.claimStatus()).isEqualTo("WAITING");
          assertThat(item.thumbnailUrl()).isEqualTo(
              "https://cdn.example.com/stored-item-15.jpg"
          );
        });
    assertThat(response.data().hasNext()).isTrue();
    assertThat(response.data().nextCursor()).isNotBlank();

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(
        Pageable.class
    );
    verify(itemClaimRepository).findAllByClaimantUserIdAndCursor(
        eq(7L),
        eq(null),
        eq(null),
        pageableCaptor.capture()
    );
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
  }

  @Test
  void decodedCursorIsPassedToRepository() {
    LocalDateTime createdAt = LocalDateTime.of(2026, 8, 12, 14, 30);
    String cursor = cursorCodec.encode(createdAt, 37L);
    when(itemClaimRepository.findAllByClaimantUserIdAndCursor(
        eq(7L),
        eq(createdAt),
        eq(37L),
        any(Pageable.class)
    )).thenReturn(List.of());

    MyItemClaimListResponse response = service.getMyItemClaims(
        7L,
        cursor,
        20
    );

    assertThat(response.data().content()).isEmpty();
    assertThat(response.data().nextCursor()).isNull();
    assertThat(response.data().hasNext()).isFalse();
    verifyNoInteractions(thumbnailService);
  }

  private ItemClaim claim(
      Long claimId,
      Long storedItemId,
      ItemClaimStatus status,
      LocalDateTime createdAt
  ) {
    ItemCategory category = mock(ItemCategory.class);
    when(category.getName()).thenReturn("Wallet");
    StoredItem storedItem = mock(StoredItem.class);
    when(storedItem.getId()).thenReturn(storedItemId);
    when(storedItem.getItemName()).thenReturn("Black wallet");
    when(storedItem.getItemCategory()).thenReturn(category);
    when(storedItem.getFoundLocationName()).thenReturn("Student Center");
    when(storedItem.getFoundDate()).thenReturn(LocalDate.of(2026, 8, 10));
    ItemClaim claim = mock(ItemClaim.class);
    when(claim.getId()).thenReturn(claimId);
    when(claim.getStoredItem()).thenReturn(storedItem);
    when(claim.getRequestMethod()).thenReturn("ONLINE");
    when(claim.getClaimStatus()).thenReturn(status);
    when(claim.getCreatedAt()).thenReturn(createdAt);
    return claim;
  }
}
