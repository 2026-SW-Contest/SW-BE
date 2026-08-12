package org.swbe.domain.facilityrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.facilityrequest.cursor.FacilityRequestCursorCodec;
import org.swbe.domain.facilityrequest.dto.response.MyFacilityRequestListResponse;
import org.swbe.domain.facilityrequest.entity.FacilityCategory;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;

class MyFacilityRequestQueryServiceTest {

  private FacilityRequestRepository facilityRequestRepository;
  private FacilityRequestThumbnailService thumbnailService;
  private FacilityRequestCursorCodec cursorCodec;
  private MyFacilityRequestQueryService service;

  @BeforeEach
  void setUp() {
    facilityRequestRepository = mock(FacilityRequestRepository.class);
    thumbnailService = mock(FacilityRequestThumbnailService.class);
    cursorCodec = new FacilityRequestCursorCodec();
    service = new MyFacilityRequestQueryService(
        facilityRequestRepository,
        thumbnailService,
        cursorCodec
    );
  }

  @Test
  void returnsCurrentUsersRequestsWithThumbnailAndCursor() {
    FacilityRequest first = request(
        25L,
        "IN_PROGRESS",
        LocalDateTime.of(2026, 8, 12, 14, 30)
    );
    FacilityRequest second = request(
        18L,
        "COMPLETED",
        LocalDateTime.of(2026, 8, 10, 11, 20)
    );
    when(facilityRequestRepository.findAllByRequesterIdAndCursor(
        eq(7L),
        eq(null),
        eq(null),
        any(Pageable.class)
    )).thenReturn(List.of(first, second));
    when(thumbnailService.resolveAll(List.of(25L))).thenReturn(
        Map.of(25L, "https://cdn.example.com/request-25.jpg")
    );

    MyFacilityRequestListResponse response =
        service.getMyFacilityRequests(7L, null, 1);

    assertThat(response.data().content()).singleElement()
        .satisfies(item -> {
          assertThat(item.facilityRequestId()).isEqualTo(25L);
          assertThat(item.requestStatus()).isEqualTo("IN_PROGRESS");
          assertThat(item.thumbnailUrl()).isEqualTo(
              "https://cdn.example.com/request-25.jpg"
          );
        });
    assertThat(response.data().hasNext()).isTrue();
    assertThat(response.data().nextCursor()).isNotBlank();

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(
        Pageable.class
    );
    verify(facilityRequestRepository).findAllByRequesterIdAndCursor(
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
    String cursor = cursorCodec.encode(createdAt, 25L);
    when(facilityRequestRepository.findAllByRequesterIdAndCursor(
        eq(7L),
        eq(createdAt),
        eq(25L),
        any(Pageable.class)
    )).thenReturn(List.of());

    MyFacilityRequestListResponse response =
        service.getMyFacilityRequests(7L, cursor, 20);

    assertThat(response.data().content()).isEmpty();
    assertThat(response.data().nextCursor()).isNull();
    assertThat(response.data().hasNext()).isFalse();
    verifyNoInteractions(thumbnailService);
  }

  private FacilityRequest request(
      Long id,
      String status,
      LocalDateTime createdAt
  ) {
    FacilityCategory category = mock(FacilityCategory.class);
    when(category.getName()).thenReturn("Electricity/Lighting");
    Location location = mock(Location.class);
    when(location.getName()).thenReturn("Student Center");
    FacilityRequest request = mock(FacilityRequest.class);
    when(request.getId()).thenReturn(id);
    when(request.getTitle()).thenReturn("Flickering hallway light");
    when(request.getFacilityCategory()).thenReturn(category);
    when(request.getLocation()).thenReturn(location);
    when(request.getRequestStatus()).thenReturn(status);
    when(request.getCreatedAt()).thenReturn(createdAt);
    return request;
  }
}
