package org.swbe.domain.facilityrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.swbe.domain.campus.entity.Building;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.facilityrequest.dto.request.AdminFacilityRequestSearchCondition;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestListItemResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestListResponse;
import org.swbe.domain.facilityrequest.entity.FacilityCategory;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.global.error.BusinessException;
import org.swbe.global.error.CommonErrorCode;

class AdminFacilityRequestQueryServiceTest {

  private FacilityRequestRepository facilityRequestRepository;
  private FacilityRequestThumbnailService thumbnailService;
  private AdminFacilityRequestQueryService service;

  @BeforeEach
  void setUp() {
    facilityRequestRepository = mock(FacilityRequestRepository.class);
    thumbnailService = mock(FacilityRequestThumbnailService.class);
    service = new AdminFacilityRequestQueryService(
        facilityRequestRepository,
        thumbnailService
    );
  }

  @Test
  void returnsPagedAdminSummaryWithRequesterAndThumbnail() {
    FacilityRequest request = request();
    Page<FacilityRequest> result = new PageImpl<>(
        List.of(request),
        PageRequest.of(0, 20),
        1
    );
    when(facilityRequestRepository.searchAdminRequests(
        any(),
        any()
    )).thenReturn(result);
    when(thumbnailService.resolveAll(List.of(25L)))
        .thenReturn(Map.of(
            25L,
            "https://cdn.example.com/request-25.jpg"
        ));
    AdminFacilityRequestSearchCondition condition = condition(
        FacilityRequestStatus.IN_PROGRESS,
        LocalDate.of(2026, 8, 1),
        LocalDate.of(2026, 8, 12)
    );

    AdminFacilityRequestListResponse response =
        service.getFacilityRequests(condition);

    assertThat(response.data().content()).hasSize(1);
    AdminFacilityRequestListItemResponse item =
        response.data().content().getFirst();
    assertThat(item.facilityRequestId()).isEqualTo(25L);
    assertThat(item.requester().name()).isEqualTo("Hong");
    assertThat(item.requester().studentNumber()).isEqualTo("60241234");
    assertThat(item.category().categoryName()).isEqualTo("Lighting");
    assertThat(item.location().locationCode()).isEqualTo("S2");
    assertThat(item.location().locationName()).isEqualTo("Student Hall");
    assertThat(item.requestStatus()).isEqualTo("IN_PROGRESS");
    assertThat(item.thumbnailUrl())
        .isEqualTo("https://cdn.example.com/request-25.jpg");
    assertThat(response.data().totalElements()).isEqualTo(1);
  }

  @Test
  void emptyResultDoesNotQueryThumbnails() {
    Page<FacilityRequest> result = new PageImpl<>(
        List.of(),
        PageRequest.of(0, 20),
        0
    );
    when(facilityRequestRepository.searchAdminRequests(
        any(),
        any()
    )).thenReturn(result);

    AdminFacilityRequestListResponse response =
        service.getFacilityRequests(condition(null, null, null));

    assertThat(response.data().content()).isEmpty();
    assertThat(response.data().totalElements()).isZero();
    verifyNoInteractions(thumbnailService);
  }

  @Test
  void startDateAfterEndDateIsRejected() {
    AdminFacilityRequestSearchCondition condition = condition(
        null,
        LocalDate.of(2026, 8, 13),
        LocalDate.of(2026, 8, 12)
    );

    assertThatThrownBy(() -> service.getFacilityRequests(condition))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(CommonErrorCode.VALIDATION_FAILED)
        );
    verifyNoInteractions(facilityRequestRepository);
  }

  private AdminFacilityRequestSearchCondition condition(
      FacilityRequestStatus status,
      LocalDate from,
      LocalDate to
  ) {
    return new AdminFacilityRequestSearchCondition(
        "  light  ",
        status,
        1L,
        2L,
        from,
        to,
        0,
        20
    );
  }

  private FacilityRequest request() {
    AppUser requester = mock(AppUser.class);
    when(requester.getId()).thenReturn(7L);
    when(requester.getName()).thenReturn("Hong");
    when(requester.getStudentNumber()).thenReturn("60241234");
    FacilityCategory category = mock(FacilityCategory.class);
    when(category.getId()).thenReturn(1L);
    when(category.getName()).thenReturn("Lighting");
    Building building = mock(Building.class);
    when(building.getCode()).thenReturn("S2");
    Location location = mock(Location.class);
    when(location.getId()).thenReturn(2L);
    when(location.getName()).thenReturn("Student Hall");
    when(location.getBuilding()).thenReturn(building);
    FacilityRequest request = mock(FacilityRequest.class);
    when(request.getId()).thenReturn(25L);
    when(request.getTitle()).thenReturn("Hallway light issue");
    when(request.getRequester()).thenReturn(requester);
    when(request.getFacilityCategory()).thenReturn(category);
    when(request.getLocation()).thenReturn(location);
    when(request.getRequestStatus()).thenReturn("IN_PROGRESS");
    when(request.getCreatedAt()).thenReturn(
        LocalDateTime.of(2026, 8, 12, 14, 30)
    );
    return request;
  }
}
