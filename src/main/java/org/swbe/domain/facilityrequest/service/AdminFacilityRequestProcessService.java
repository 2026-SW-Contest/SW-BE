package org.swbe.domain.facilityrequest.service;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.facilityrequest.dto.request.AdminFacilityRequestProcessRequest;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestAdminResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestProcessDataResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestProcessResponse;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.entity.RequestComment;
import org.swbe.domain.facilityrequest.exception.FacilityRequestErrorCode;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.domain.facilityrequest.repository.RequestCommentRepository;
import org.swbe.domain.notification.entity.Notification;
import org.swbe.domain.notification.repository.NotificationRepository;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.exception.UserErrorCode;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.global.error.BusinessException;

@Service
public class AdminFacilityRequestProcessService {

  private final FacilityRequestRepository facilityRequestRepository;
  private final RequestCommentRepository requestCommentRepository;
  private final NotificationRepository notificationRepository;
  private final AppUserRepository appUserRepository;
  private final Clock clock;

  public AdminFacilityRequestProcessService(
      FacilityRequestRepository facilityRequestRepository,
      RequestCommentRepository requestCommentRepository,
      NotificationRepository notificationRepository,
      AppUserRepository appUserRepository,
      Clock clock
  ) {
    this.facilityRequestRepository = facilityRequestRepository;
    this.requestCommentRepository = requestCommentRepository;
    this.notificationRepository = notificationRepository;
    this.appUserRepository = appUserRepository;
    this.clock = clock;
  }

  // 관리자가 시설문의 상태를 변경하고 공개 답변과 사용자 알림을 저장한다.
  @Transactional
  public AdminFacilityRequestProcessResponse process(
      Long facilityRequestId,
      AdminFacilityRequestProcessRequest processRequest,
      Long administratorUserId
  ) {
    validateUpdateExists(processRequest);
    FacilityRequest facilityRequest = facilityRequestRepository
        .findAdminDetailById(facilityRequestId)
        .orElseThrow(() -> new BusinessException(
            FacilityRequestErrorCode.NOT_FOUND
        ));
    validateNotCompleted(facilityRequest);

    String previousStatus = facilityRequest.getRequestStatus();
    LocalDateTime now = LocalDateTime.now(clock);
    boolean statusChanged = changeStatusIfRequested(
        facilityRequest,
        processRequest.status(),
        processRequest.hasAdminResponse(),
        now
    );
    AdminFacilityRequestAdminResponse adminResponse =
        createAdminResponseIfRequested(
            facilityRequest,
            processRequest,
            administratorUserId,
            now
        );

    if (!statusChanged) {
      facilityRequest.touch(now);
    }
    createNotification(
        facilityRequest,
        processRequest,
        statusChanged,
        now
    );
    FacilityRequestStatus currentStatus = FacilityRequestStatus.valueOf(
        facilityRequest.getRequestStatus()
    );
    AdminFacilityRequestProcessDataResponse data =
        new AdminFacilityRequestProcessDataResponse(
            facilityRequest.getId(),
            previousStatus,
            facilityRequest.getRequestStatus(),
            currentStatus.getDisplayName(),
            adminResponse,
            facilityRequest.getUpdatedAt()
        );

    return new AdminFacilityRequestProcessResponse(data);
  }

  // 상태와 답변이 모두 비어 있는 요청을 거부한다.
  private void validateUpdateExists(
      AdminFacilityRequestProcessRequest request
  ) {
    if (request.status() == null && !request.hasAdminResponse()) {
      throw new BusinessException(
          FacilityRequestErrorCode.UPDATE_REQUIRED
      );
    }
  }

  // 처리 완료된 문의가 다시 변경되지 않도록 확인한다.
  private void validateNotCompleted(FacilityRequest facilityRequest) {
    if (facilityRequest.isCompleted()) {
      throw new BusinessException(
          FacilityRequestErrorCode.ALREADY_COMPLETED
      );
    }
  }

  // 요청 상태가 실제로 변경되는 경우 엔터티의 전환 규칙을 적용한다.
  private boolean changeStatusIfRequested(
      FacilityRequest facilityRequest,
      FacilityRequestStatus requestedStatus,
      boolean hasAdminResponse,
      LocalDateTime now
  ) {
    if (requestedStatus == null) {
      return false;
    }
    if (facilityRequest.hasStatus(requestedStatus)) {
      if (!hasAdminResponse) {
        throw new BusinessException(
            FacilityRequestErrorCode.UPDATE_REQUIRED
        );
      }
      return false;
    }
    if (!facilityRequest.canTransitionTo(requestedStatus)) {
      throw new BusinessException(
          FacilityRequestErrorCode.INVALID_STATUS_TRANSITION
      );
    }

    facilityRequest.transitionTo(requestedStatus, now);
    return true;
  }

  // 답변이 전달된 경우 작성 관리자를 연결한 공개 답변을 저장한다.
  private AdminFacilityRequestAdminResponse createAdminResponseIfRequested(
      FacilityRequest facilityRequest,
      AdminFacilityRequestProcessRequest processRequest,
      Long administratorUserId,
      LocalDateTime now
  ) {
    if (!processRequest.hasAdminResponse()) {
      return null;
    }
    AppUser administrator = appUserRepository.findById(administratorUserId)
        .orElseThrow(() -> new BusinessException(
            UserErrorCode.NOT_FOUND
        ));
    RequestComment savedComment = requestCommentRepository.save(
        RequestComment.createAdminResponse(
            facilityRequest,
            administrator,
            processRequest.adminResponse(),
            now
        )
    );

    return new AdminFacilityRequestAdminResponse(
        savedComment.getId(),
        savedComment.getContent(),
        savedComment.getCreatedAt()
    );
  }

  // 상태 변경 또는 답변 등록 결과를 문의 작성자에게 알린다.
  private void createNotification(
      FacilityRequest facilityRequest,
      AdminFacilityRequestProcessRequest processRequest,
      boolean statusChanged,
      LocalDateTime now
  ) {
    String title = statusChanged
        ? "시설문의 처리 상태가 변경되었습니다."
        : "시설문의에 관리자 답변이 등록되었습니다.";
    String content = processRequest.hasAdminResponse()
        ? processRequest.adminResponse()
        : "시설문의 상태가 변경되었습니다.";
    Notification notification = Notification.createFacilityRequestUpdate(
        facilityRequest.getRequester(),
        facilityRequest.getId(),
        title,
        content,
        now
    );
    notificationRepository.save(notification);
  }
}
