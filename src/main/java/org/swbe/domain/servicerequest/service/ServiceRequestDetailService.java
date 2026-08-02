package org.swbe.domain.servicerequest.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestAttachmentResponse;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestCategoryDetailResponse;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestDetailDataResponse;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestDetailResponse;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestLocationDetailResponse;
import org.swbe.domain.servicerequest.entity.ServiceRequest;
import org.swbe.domain.servicerequest.entity.ServiceRequestAttachment;
import org.swbe.domain.servicerequest.entity.ServiceRequestStatus;
import org.swbe.domain.servicerequest.exception.ServiceRequestErrorCode;
import org.swbe.domain.servicerequest.repository.RequestAssignmentRepository;
import org.swbe.domain.servicerequest.repository.ServiceRequestAttachmentRepository;
import org.swbe.domain.servicerequest.repository.ServiceRequestRepository;
import org.swbe.global.error.BusinessException;

@Service
@Transactional(readOnly = true)
public class ServiceRequestDetailService {

  private static final String PUBLIC_VISIBILITY = "PUBLIC";

  private final ServiceRequestRepository serviceRequestRepository;
  private final RequestAssignmentRepository requestAssignmentRepository;
  private final ServiceRequestAttachmentRepository attachmentRepository;

  public ServiceRequestDetailService(
      ServiceRequestRepository serviceRequestRepository,
      RequestAssignmentRepository requestAssignmentRepository,
      ServiceRequestAttachmentRepository attachmentRepository
  ) {
    this.serviceRequestRepository = serviceRequestRepository;
    this.requestAssignmentRepository = requestAssignmentRepository;
    this.attachmentRepository = attachmentRepository;
  }

  public ServiceRequestDetailResponse getServiceRequest(
      Long serviceRequestId,
      Long viewerUserId,
      boolean administrator
  ) {
    ServiceRequest request = serviceRequestRepository
        .findDetailById(serviceRequestId)
        .orElseThrow(this::notFound);
    boolean owner = isOwner(request, viewerUserId);
    if (!canView(request, serviceRequestId, viewerUserId, owner, administrator)) {
      throw notFound();
    }

    List<ServiceRequestAttachmentResponse> attachments = attachmentRepository
        .findAllByServiceRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(
            serviceRequestId
        )
        .stream()
        .map(this::toAttachmentResponse)
        .toList();
    ServiceRequestStatus status = ServiceRequestStatus.valueOf(
        request.getRequestStatus()
    );
    boolean editable = owner && status == ServiceRequestStatus.RECEIVED;

    var data = new ServiceRequestDetailDataResponse(
        request.getId(),
        request.getReceiptNumber(),
        request.getTitle(),
        request.getDescription(),
        request.getEquipmentName(),
        new ServiceRequestCategoryDetailResponse(
            request.getRequestCategory().getId(),
            request.getRequestCategory().getName()
        ),
        new ServiceRequestLocationDetailResponse(
            request.getLocation().getId(),
            request.getLocation().getName()
        ),
        request.getRequestStatus(),
        status.getDisplayName(),
        attachments,
        editable,
        editable,
        request.getCreatedAt(),
        request.getUpdatedAt()
    );

    return new ServiceRequestDetailResponse(data);
  }

  private boolean isPublic(ServiceRequest request) {
    return PUBLIC_VISIBILITY.equals(request.getVisibility());
  }

  private boolean isOwner(ServiceRequest request, Long viewerUserId) {
    return viewerUserId != null
        && viewerUserId.equals(request.getRequester().getId());
  }

  private boolean canView(
      ServiceRequest request,
      Long serviceRequestId,
      Long viewerUserId,
      boolean owner,
      boolean administrator
  ) {
    return isPublic(request)
        || owner
        || administrator
        || isAssignedStaff(serviceRequestId, viewerUserId);
  }

  private boolean isAssignedStaff(
      Long serviceRequestId,
      Long viewerUserId
  ) {
    return viewerUserId != null
        && requestAssignmentRepository
        .existsByServiceRequest_IdAndAssignedUser_IdAndEndedAtIsNull(
            serviceRequestId,
            viewerUserId
        );
  }

  private ServiceRequestAttachmentResponse toAttachmentResponse(
      ServiceRequestAttachment attachment
  ) {
    FileResource file = attachment.getFile();
    return new ServiceRequestAttachmentResponse(
        file.getId(),
        file.getOriginalFilename(),
        null
    );
  }

  private BusinessException notFound() {
    return new BusinessException(ServiceRequestErrorCode.NOT_FOUND);
  }
}
