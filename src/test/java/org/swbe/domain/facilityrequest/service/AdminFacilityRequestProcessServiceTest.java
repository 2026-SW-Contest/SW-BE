package org.swbe.domain.facilityrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.swbe.domain.facilityrequest.dto.request.AdminFacilityRequestProcessRequest;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestProcessResponse;
import org.swbe.domain.facilityrequest.entity.FacilityCategory;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.entity.RequestComment;
import org.swbe.domain.facilityrequest.exception.FacilityRequestErrorCode;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.domain.facilityrequest.repository.RequestCommentRepository;
import org.swbe.domain.notification.entity.Notification;
import org.swbe.domain.notification.repository.NotificationRepository;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.global.error.BusinessException;

class AdminFacilityRequestProcessServiceTest {

  private static final LocalDateTime NOW =
      LocalDateTime.of(2026, 8, 12, 16, 30);

  private FacilityRequestRepository facilityRequestRepository;
  private RequestCommentRepository requestCommentRepository;
  private NotificationRepository notificationRepository;
  private AppUserRepository appUserRepository;
  private AdminFacilityRequestProcessService service;

  @BeforeEach
  void setUp() {
    facilityRequestRepository = mock(FacilityRequestRepository.class);
    requestCommentRepository = mock(RequestCommentRepository.class);
    notificationRepository = mock(NotificationRepository.class);
    appUserRepository = mock(AppUserRepository.class);
    Clock clock = Clock.fixed(
        Instant.parse("2026-08-12T16:30:00Z"),
        ZoneOffset.UTC
    );
    service = new AdminFacilityRequestProcessService(
        facilityRequestRepository,
        requestCommentRepository,
        notificationRepository,
        appUserRepository,
        clock
    );
  }

  @Test
  void changesStatusAndCreatesResponseAndNotification() {
    AppUser requester = mock(AppUser.class);
    AppUser administrator = mock(AppUser.class);
    FacilityRequest facilityRequest = request(requester, "WAITING");
    when(facilityRequestRepository.findAdminDetailById(25L))
        .thenReturn(Optional.of(facilityRequest));
    when(appUserRepository.findById(7L))
        .thenReturn(Optional.of(administrator));
    when(requestCommentRepository.save(any(RequestComment.class)))
        .thenAnswer(invocation -> {
          RequestComment comment = invocation.getArgument(0);
          ReflectionTestUtils.setField(comment, "id", 3L);
          return comment;
        });
    AdminFacilityRequestProcessRequest request =
        new AdminFacilityRequestProcessRequest(
            FacilityRequestStatus.IN_PROGRESS,
            "  Inspection started.  "
        );

    AdminFacilityRequestProcessResponse response = service.process(
        25L,
        request,
        7L
    );

    assertThat(response.data().previousStatus()).isEqualTo("WAITING");
    assertThat(response.data().requestStatus()).isEqualTo("IN_PROGRESS");
    assertThat(response.data().requestStatusName()).isEqualTo("진행중");
    assertThat(response.data().adminResponse().responseId()).isEqualTo(3L);
    assertThat(response.data().adminResponse().content())
        .isEqualTo("Inspection started.");
    assertThat(facilityRequest.getUpdatedAt()).isEqualTo(NOW);
    verify(notificationRepository).save(any(Notification.class));
  }

  @Test
  void completesWaitingFacilityRequestWithoutResponse() {
    FacilityRequest facilityRequest = request(mock(AppUser.class), "WAITING");
    when(facilityRequestRepository.findAdminDetailById(25L))
        .thenReturn(Optional.of(facilityRequest));

    AdminFacilityRequestProcessResponse response = service.process(
        25L,
        new AdminFacilityRequestProcessRequest(
            FacilityRequestStatus.COMPLETED,
            null
        ),
        7L
    );

    assertThat(response.data().requestStatus()).isEqualTo("COMPLETED");
    assertThat(response.data().requestStatusName()).isEqualTo("완료");
    assertThat(response.data().adminResponse()).isNull();
    assertThat(facilityRequest.getCompletedAt()).isEqualTo(NOW);
    verifyNoInteractions(appUserRepository);
  }

  @Test
  void emptyProcessRequestIsRejected() {
    AdminFacilityRequestProcessRequest request =
        new AdminFacilityRequestProcessRequest(null, null);

    assertThatThrownBy(() -> service.process(25L, request, 7L))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(FacilityRequestErrorCode.UPDATE_REQUIRED)
        );
    verifyNoInteractions(facilityRequestRepository);
  }

  @Test
  void completedFacilityRequestCannotBeProcessed() {
    FacilityRequest facilityRequest = request(
        mock(AppUser.class),
        "COMPLETED"
    );
    when(facilityRequestRepository.findAdminDetailById(25L))
        .thenReturn(Optional.of(facilityRequest));

    assertThatThrownBy(() -> service.process(
        25L,
        new AdminFacilityRequestProcessRequest(
            null,
            "Additional response"
        ),
        7L
    )).isInstanceOfSatisfying(BusinessException.class, exception ->
        assertThat(exception.getErrorCode())
            .isEqualTo(FacilityRequestErrorCode.ALREADY_COMPLETED)
    );
  }

  @Test
  void statusCannotMoveBackward() {
    FacilityRequest facilityRequest = request(
        mock(AppUser.class),
        "IN_PROGRESS"
    );
    when(facilityRequestRepository.findAdminDetailById(25L))
        .thenReturn(Optional.of(facilityRequest));

    assertThatThrownBy(() -> service.process(
        25L,
        new AdminFacilityRequestProcessRequest(
            FacilityRequestStatus.WAITING,
            null
        ),
        7L
    )).isInstanceOfSatisfying(BusinessException.class, exception ->
        assertThat(exception.getErrorCode())
            .isEqualTo(FacilityRequestErrorCode.INVALID_STATUS_TRANSITION)
    );
  }

  private FacilityRequest request(AppUser requester, String status) {
    FacilityRequest facilityRequest = FacilityRequest.create(
        mock(FacilityCategory.class),
        mock(Location.class),
        requester,
        "Hallway light issue",
        "The hallway light keeps flickering.",
        LocalDateTime.of(2026, 8, 12, 14, 30)
    );
    ReflectionTestUtils.setField(facilityRequest, "id", 25L);
    ReflectionTestUtils.setField(facilityRequest, "requestStatus", status);
    return facilityRequest;
  }
}
