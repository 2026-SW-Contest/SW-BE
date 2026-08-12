package org.swbe.domain.facilityrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListResponse;
import org.swbe.domain.facilityrequest.entity.FacilityCategory;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;

class MyFacilityRequestQueryServiceTest {

  private FacilityRequestRepository facilityRequestRepository;
  private FacilityRequestThumbnailService thumbnailService;
  private MyFacilityRequestQueryService service;

  @BeforeEach
  void setUp() {
    facilityRequestRepository = mock(FacilityRequestRepository.class);
    thumbnailService = mock(FacilityRequestThumbnailService.class);
    service = new MyFacilityRequestQueryService(
        facilityRequestRepository,
        thumbnailService
    );
  }

  @Test
  void returnsOnlyCurrentUsersRequestsWithThumbnail() {
    FacilityRequest request = request(25L, "IN_PROGRESS");
    when(facilityRequestRepository.findAllByRequester_Id(
        org.mockito.ArgumentMatchers.eq(7L),
        org.mockito.ArgumentMatchers.any(PageRequest.class)
    )).thenReturn(new PageImpl<>(
        List.of(request),
        PageRequest.of(0, 20),
        1
    ));
    when(thumbnailService.resolveAll(List.of(25L))).thenReturn(
        Map.of(25L, "https://cdn.example.com/request-25.jpg")
    );

    FacilityRequestListResponse response =
        service.getMyFacilityRequests(7L, 0, 20);

    assertThat(response.data().content()).hasSize(1);
    assertThat(response.data().content().getFirst().facilityRequestId())
        .isEqualTo(25L);
    assertThat(response.data().content().getFirst().requestStatus())
        .isEqualTo("IN_PROGRESS");
    assertThat(response.data().content().getFirst().thumbnailUrl())
        .isEqualTo("https://cdn.example.com/request-25.jpg");
    assertThat(response.data().page()).isZero();
    assertThat(response.data().totalElements()).isEqualTo(1);

    ArgumentCaptor<PageRequest> pageableCaptor =
        ArgumentCaptor.forClass(PageRequest.class);
    verify(facilityRequestRepository).findAllByRequester_Id(
        org.mockito.ArgumentMatchers.eq(7L),
        pageableCaptor.capture()
    );
    assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt"))
        .isNotNull()
        .satisfies(order -> assertThat(order.isDescending()).isTrue());
  }

  @Test
  void returnsEmptyPageWithoutLookingUpThumbnails() {
    when(facilityRequestRepository.findAllByRequester_Id(
        org.mockito.ArgumentMatchers.eq(7L),
        org.mockito.ArgumentMatchers.any(PageRequest.class)
    )).thenReturn(new PageImpl<>(
        List.of(),
        PageRequest.of(0, 20),
        0
    ));

    FacilityRequestListResponse response =
        service.getMyFacilityRequests(7L, 0, 20);

    assertThat(response.data().content()).isEmpty();
    assertThat(response.data().totalElements()).isZero();
    assertThat(response.data().hasNext()).isFalse();
    verifyNoInteractions(thumbnailService);
  }

  private FacilityRequest request(Long id, String status) {
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
    when(request.getCreatedAt()).thenReturn(
        LocalDateTime.of(2026, 8, 12, 14, 30)
    );
    return request;
  }
}
