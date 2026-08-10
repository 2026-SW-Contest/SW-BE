package org.swbe.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.facilityrequest.entity.FacilityCategory;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.domain.facilityrequest.service.FacilityRequestThumbnailService;
import org.swbe.domain.lostitem.entity.ItemCategory;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.domain.lostitem.service.StoredItemThumbnailService;
import org.swbe.domain.search.cursor.SearchCursorCodec;

class IntegratedSearchServiceTest {

  private StoredItemRepository storedItemRepository;
  private FacilityRequestRepository facilityRequestRepository;
  private SearchCursorCodec searchCursorCodec;
  private StoredItemThumbnailService storedItemThumbnailService;
  private FacilityRequestThumbnailService facilityRequestThumbnailService;
  private IntegratedSearchService service;

  @BeforeEach
  void setUp() {
    storedItemRepository = mock(StoredItemRepository.class);
    facilityRequestRepository = mock(FacilityRequestRepository.class);
    searchCursorCodec = mock(SearchCursorCodec.class);
    storedItemThumbnailService = mock(StoredItemThumbnailService.class);
    facilityRequestThumbnailService = mock(
        FacilityRequestThumbnailService.class
    );
    service = new IntegratedSearchService(
        storedItemRepository,
        facilityRequestRepository,
        searchCursorCodec,
        storedItemThumbnailService,
        facilityRequestThumbnailService
    );
  }

  @Test
  void summaryContainsCountsFromBothDomains() {
    when(storedItemRepository.countSearchMatches("%에어%"))
        .thenReturn(12L);
    when(facilityRequestRepository
        .countIntegratedSearchMatches("%에어%"))
        .thenReturn(4L);

    var response = service.getSummary(" 에어 ");

    assertThat(response.data().keyword()).isEqualTo("에어");
    assertThat(response.data().lostItemCount()).isEqualTo(12L);
    assertThat(response.data().facilityRequestCount()).isEqualTo(4L);
  }

  @Test
  void lostItemSearchUsesOneExtraRowToCreateNextCursor() {
    StoredItem first = lostItem(30L, "에어팟 프로", 30);
    StoredItem second = lostItem(20L, "에어팟", 20);
    StoredItem extra = lostItem(10L, "에어팟 케이스", 10);
    when(storedItemRepository.searchByCursor(
        eq("%에어%"),
        isNull(),
        isNull(),
        any(Pageable.class)
    )).thenReturn(List.of(first, second, extra));
    when(searchCursorCodec.encode(
        second.getCreatedAt(),
        second.getId()
    )).thenReturn("next-cursor");
    when(storedItemThumbnailService.resolveAll(List.of(30L, 20L)))
        .thenReturn(Map.of(
            30L,
            "https://cdn.example.com/lost-item.jpg"
        ));

    var response = service.searchLostItems("에어", null, 2);

    assertThat(response.data().content()).hasSize(2);
    assertThat(response.data().content().getFirst().itemName())
        .isEqualTo("에어팟 프로");
    assertThat(response.data().content().getFirst().thumbnailUrl())
        .isEqualTo("https://cdn.example.com/lost-item.jpg");
    assertThat(response.data().content().get(1).thumbnailUrl()).isNull();
    assertThat(response.data().nextCursor())
        .isEqualTo("next-cursor");
    assertThat(response.data().hasNext()).isTrue();
    verify(searchCursorCodec).encode(
        second.getCreatedAt(),
        second.getId()
    );
  }

  @Test
  void facilityRequestWithoutNextRowHasNoCursor() {
    FacilityRequest request = facilityRequest();
    when(facilityRequestRepository.searchIntegratedByCursor(
        eq("%에어%"),
        isNull(),
        isNull(),
        any(Pageable.class)
    )).thenReturn(List.of(request));
    when(facilityRequestThumbnailService.resolveAll(List.of(40L)))
        .thenReturn(Map.of(
            40L,
            "https://cdn.example.com/facility-request.jpg"
        ));

    var response = service.searchFacilityRequests(
        "에어",
        null,
        20
    );

    assertThat(response.data().content()).singleElement()
        .satisfies(item -> {
          assertThat(item.title()).isEqualTo("에어컨 고장");
          assertThat(item.requestStatusName()).isEqualTo("진행중");
          assertThat(item.thumbnailUrl()).isEqualTo(
              "https://cdn.example.com/facility-request.jpg"
          );
        });
    assertThat(response.data().nextCursor()).isNull();
    assertThat(response.data().hasNext()).isFalse();
  }

  private StoredItem lostItem(
      Long id,
      String name,
      int minute
  ) {
    ItemCategory category = mock(ItemCategory.class);
    when(category.getName()).thenReturn("전자기기");
    StoredItem item = mock(StoredItem.class);
    when(item.getId()).thenReturn(id);
    when(item.getItemName()).thenReturn(name);
    when(item.getItemCategory()).thenReturn(category);
    when(item.getFoundDate()).thenReturn(LocalDate.of(2026, 8, 8));
    when(item.getPublicStatus()).thenReturn("STORED");
    when(item.getCreatedAt()).thenReturn(
        LocalDateTime.of(2026, 8, 8, 12, minute)
    );
    return item;
  }

  private FacilityRequest facilityRequest() {
    FacilityCategory category = mock(FacilityCategory.class);
    when(category.getName()).thenReturn("냉난방");
    Location location = mock(Location.class);
    when(location.getName()).thenReturn("명진당 301호");
    FacilityRequest request = mock(FacilityRequest.class);
    when(request.getId()).thenReturn(40L);
    when(request.getTitle()).thenReturn("에어컨 고장");
    when(request.getDescription()).thenReturn("찬 바람이 나오지 않습니다.");
    when(request.getFacilityCategory()).thenReturn(category);
    when(request.getLocation()).thenReturn(location);
    when(request.getRequestStatus()).thenReturn("IN_PROGRESS");
    when(request.getCreatedAt()).thenReturn(
        LocalDateTime.of(2026, 8, 8, 12, 0)
    );
    return request;
  }
}
