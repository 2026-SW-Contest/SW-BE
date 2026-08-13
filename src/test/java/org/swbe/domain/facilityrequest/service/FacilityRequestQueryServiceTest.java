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
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.facilityrequest.cursor.FacilityRequestCursor;
import org.swbe.domain.facilityrequest.cursor.FacilityRequestCursorCodec;
import org.swbe.domain.facilityrequest.dto.request.FacilityRequestSearchCondition;
import org.swbe.domain.facilityrequest.entity.FacilityCategory;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.exception.FacilityRequestErrorCode;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.global.error.BusinessException;
import org.swbe.global.error.CommonErrorCode;

@ExtendWith(MockitoExtension.class)
class FacilityRequestQueryServiceTest {

  @Mock
  private FacilityRequestRepository facilityRequestRepository;

  @Mock
  private FacilityRequestThumbnailService thumbnailService;

  @Mock
  private FacilityRequestCursorCodec cursorCodec;

  @InjectMocks
  private FacilityRequestQueryService service;

  @Test
  void returnsFilteredRequestsWithNextCursor() {
    FacilityRequest first = request(25L, LocalDateTime.of(
        2026, 8, 1, 16, 0
    ));
    FacilityRequest extra = mock(FacilityRequest.class);
    when(facilityRequestRepository.searchRequestsByCursor(
        eq(1L),
        eq(2L),
        eq("IN_PROGRESS"),
        eq("조명"),
        eq(LocalDateTime.of(2026, 7, 1, 0, 0)),
        eq(LocalDateTime.of(2026, 8, 2, 0, 0)),
        eq(null),
        eq(null),
        any(Pageable.class)
    )).thenReturn(List.of(first, extra));
    when(thumbnailService.resolveAll(List.of(25L))).thenReturn(
        Map.of(25L, "https://cdn.example.com/image.jpg")
    );
    when(cursorCodec.encode(first.getCreatedAt(), 25L))
        .thenReturn("next-cursor");

    var response = service.getFacilityRequests(condition(
        null,
        1
    ));

    assertThat(response.data().content()).singleElement()
        .satisfies(item -> {
          assertThat(item.facilityRequestId()).isEqualTo(25L);
          assertThat(item.title()).isEqualTo("학생회관 1층 조명 깜빡임");
          assertThat(item.categoryName()).isEqualTo("전기/조명");
          assertThat(item.locationName()).isEqualTo("학생회관");
          assertThat(item.requestStatus()).isEqualTo("IN_PROGRESS");
          assertThat(item.thumbnailUrl())
              .isEqualTo("https://cdn.example.com/image.jpg");
        });
    assertThat(response.data().nextCursor()).isEqualTo("next-cursor");
    assertThat(response.data().hasNext()).isTrue();
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(
        Pageable.class
    );
    verify(facilityRequestRepository).searchRequestsByCursor(
        eq(1L),
        eq(2L),
        eq("IN_PROGRESS"),
        eq("조명"),
        eq(LocalDateTime.of(2026, 7, 1, 0, 0)),
        eq(LocalDateTime.of(2026, 8, 2, 0, 0)),
        eq(null),
        eq(null),
        pageableCaptor.capture()
    );
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
  }

  @Test
  void decodedCursorIsPassedToRepository() {
    LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 16, 0);
    when(cursorCodec.decode("cursor"))
        .thenReturn(new FacilityRequestCursor(createdAt, 25L));
    when(facilityRequestRepository.searchRequestsByCursor(
        eq(1L),
        eq(2L),
        eq("IN_PROGRESS"),
        eq("조명"),
        any(),
        any(),
        eq(createdAt),
        eq(25L),
        any(Pageable.class)
    )).thenReturn(List.of());

    var response = service.getFacilityRequests(condition("cursor", 20));

    assertThat(response.data().content()).isEmpty();
    assertThat(response.data().nextCursor()).isNull();
    assertThat(response.data().hasNext()).isFalse();
  }

  @Test
  void invalidCursorIsRejected() {
    BusinessException exception = new BusinessException(
        FacilityRequestErrorCode.INVALID_CURSOR
    );
    when(cursorCodec.decode("invalid")).thenThrow(exception);

    assertThatThrownBy(
        () -> service.getFacilityRequests(condition("invalid", 20))
    ).isSameAs(exception);
    verifyNoInteractions(facilityRequestRepository);
  }

  @Test
  void startDateAfterEndDateIsRejected() {
    FacilityRequestSearchCondition condition =
        new FacilityRequestSearchCondition(
            null,
            null,
            null,
            null,
            LocalDate.of(2026, 8, 2),
            LocalDate.of(2026, 8, 1),
            null,
            20
        );

    assertThatThrownBy(() -> service.getFacilityRequests(condition))
        .isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode()
        ).isEqualTo(CommonErrorCode.VALIDATION_FAILED));
    verifyNoInteractions(facilityRequestRepository);
  }

  private FacilityRequestSearchCondition condition(
      String cursor,
      int size
  ) {
    return new FacilityRequestSearchCondition(
        1L,
        2L,
        FacilityRequestStatus.IN_PROGRESS,
        "  조명  ",
        LocalDate.of(2026, 7, 1),
        LocalDate.of(2026, 8, 1),
        cursor,
        size
    );
  }

  private FacilityRequest request(Long id, LocalDateTime createdAt) {
    FacilityCategory category = mock(FacilityCategory.class);
    Location location = mock(Location.class);
    FacilityRequest request = mock(FacilityRequest.class);
    when(category.getName()).thenReturn("전기/조명");
    when(location.getName()).thenReturn("학생회관");
    when(request.getId()).thenReturn(id);
    when(request.getTitle()).thenReturn("학생회관 1층 조명 깜빡임");
    when(request.getFacilityCategory()).thenReturn(category);
    when(request.getLocation()).thenReturn(location);
    when(request.getRequestStatus()).thenReturn("IN_PROGRESS");
    when(request.getCreatedAt()).thenReturn(createdAt);
    return request;
  }
}
