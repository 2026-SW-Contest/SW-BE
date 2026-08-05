package org.swbe.domain.facilityrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.facilityrequest.dto.request.FacilityRequestSearchCondition;
import org.swbe.domain.facilityrequest.entity.FacilityCategory;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.global.error.BusinessException;
import org.swbe.global.error.CommonErrorCode;

@ExtendWith(MockitoExtension.class)
class FacilityRequestQueryServiceTest {

  @Mock
  private FacilityRequestRepository facilityRequestRepository;

  @InjectMocks
  private FacilityRequestQueryService facilityRequestQueryService;

  @Test
  void publicRequestsAreReturnedAsPagedSummary() {
    LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 16, 0);
    FacilityCategory category = mock(FacilityCategory.class);
    when(category.getName()).thenReturn("전기/조명");
    Location location = mock(Location.class);
    when(location.getName()).thenReturn("학생회관");
    FacilityRequest request = mock(FacilityRequest.class);
    when(request.getId()).thenReturn(25L);
    when(request.getTitle()).thenReturn("학생회관 1층 조명 깜빡임");
    when(request.getFacilityCategory()).thenReturn(category);
    when(request.getLocation()).thenReturn(location);
    when(request.getRequestStatus()).thenReturn("IN_PROGRESS");
    when(request.getCreatedAt()).thenReturn(createdAt);
    var page = new PageImpl<>(
        List.of(request),
        PageRequest.of(0, 20),
        1
    );
    when(facilityRequestRepository.searchPublicRequests(
        eq(1L),
        eq(2L),
        eq("IN_PROGRESS"),
        eq("조명"),
        eq(LocalDateTime.of(2026, 7, 1, 0, 0)),
        eq(LocalDateTime.of(2026, 8, 2, 0, 0)),
        any(Pageable.class)
    )).thenReturn(page);
    var condition = new FacilityRequestSearchCondition(
        1L,
        2L,
        FacilityRequestStatus.IN_PROGRESS,
        "  조명  ",
        LocalDate.of(2026, 7, 1),
        LocalDate.of(2026, 8, 1),
        0,
        20
    );

    var response = facilityRequestQueryService.getFacilityRequests(condition);

    assertThat(response.data().content()).hasSize(1);
    var item = response.data().content().getFirst();
    assertThat(item.facilityRequestId()).isEqualTo(25L);
    assertThat(item.title()).isEqualTo("학생회관 1층 조명 깜빡임");
    assertThat(item.categoryName()).isEqualTo("전기/조명");
    assertThat(item.locationName()).isEqualTo("학생회관");
    assertThat(item.requestStatus()).isEqualTo("IN_PROGRESS");
    assertThat(item.requestStatusName()).isEqualTo("진행중");
    assertThat(item.thumbnailUrl()).isNull();
    assertThat(item.createdAt()).isEqualTo(createdAt);
    assertThat(response.data().page()).isZero();
    assertThat(response.data().size()).isEqualTo(20);
    assertThat(response.data().totalElements()).isEqualTo(1);
    assertThat(response.data().totalPages()).isEqualTo(1);
    assertThat(response.data().hasNext()).isFalse();
    verify(facilityRequestRepository).searchPublicRequests(
        eq(1L),
        eq(2L),
        eq("IN_PROGRESS"),
        eq("조명"),
        eq(LocalDateTime.of(2026, 7, 1, 0, 0)),
        eq(LocalDateTime.of(2026, 8, 2, 0, 0)),
        any(Pageable.class)
    );
  }

  @Test
  void noSearchResultReturnsEmptyPage() {
    var page = new PageImpl<FacilityRequest>(
        List.of(),
        PageRequest.of(0, 20),
        0
    );
    when(facilityRequestRepository.searchPublicRequests(
        eq(null),
        eq(null),
        eq(null),
        eq(null),
        eq(null),
        eq(null),
        any(Pageable.class)
    )).thenReturn(page);
    var condition = new FacilityRequestSearchCondition(
        null,
        null,
        null,
        " ",
        null,
        null,
        0,
        20
    );

    var response = facilityRequestQueryService.getFacilityRequests(condition);

    assertThat(response.data().content()).isEmpty();
    assertThat(response.data().totalElements()).isZero();
    assertThat(response.data().totalPages()).isZero();
    assertThat(response.data().hasNext()).isFalse();
  }

  @Test
  void startDateAfterEndDateIsRejected() {
    var condition = new FacilityRequestSearchCondition(
        null,
        null,
        null,
        null,
        LocalDate.of(2026, 8, 2),
        LocalDate.of(2026, 8, 1),
        0,
        20
    );

    assertThatThrownBy(
        () -> facilityRequestQueryService.getFacilityRequests(condition)
    )
        .isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode()
        ).isEqualTo(CommonErrorCode.VALIDATION_FAILED));
    verifyNoInteractions(facilityRequestRepository);
  }
}
