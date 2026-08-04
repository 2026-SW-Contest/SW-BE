package org.swbe.domain.facilityrequest.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestAttachmentResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityCategoryResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestDetailDataResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestDetailResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestLocationDetailResponse;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestAttachment;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.exception.FacilityRequestErrorCode;
import org.swbe.domain.facilityrequest.repository.RequestAssignmentRepository;
import org.swbe.domain.facilityrequest.repository.FacilityRequestAttachmentRepository;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.global.error.BusinessException;

@Service
@Transactional(readOnly = true)
public class FacilityRequestDetailService {

  private static final String PUBLIC_VISIBILITY = "PUBLIC";

  private final FacilityRequestRepository facilityRequestRepository;
  private final RequestAssignmentRepository requestAssignmentRepository;
  private final FacilityRequestAttachmentRepository attachmentRepository;

  public FacilityRequestDetailService(
      FacilityRequestRepository facilityRequestRepository,
      RequestAssignmentRepository requestAssignmentRepository,
      FacilityRequestAttachmentRepository attachmentRepository
  ) {
    this.facilityRequestRepository = facilityRequestRepository;
    this.requestAssignmentRepository = requestAssignmentRepository;
    this.attachmentRepository = attachmentRepository;
  }

  public FacilityRequestDetailResponse getFacilityRequest(
      Long facilityRequestId,
      Long viewerUserId,
      boolean administrator
  ) {
    FacilityRequest request = facilityRequestRepository
        .findDetailById(facilityRequestId)
        .orElseThrow(this::notFound);
    boolean owner = request.isRequestedBy(viewerUserId);
    if (!canView(request, facilityRequestId, viewerUserId, owner, administrator)) {
      throw notFound();
    }

    List<FacilityRequestAttachmentResponse> attachments = attachmentRepository
        .findAllByFacilityRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(
            facilityRequestId
        )
        .stream()
        .map(this::toAttachmentResponse)
        .toList();
    FacilityRequestStatus status = FacilityRequestStatus.valueOf(
        request.getRequestStatus()
    );
    boolean editable = owner && status == FacilityRequestStatus.RECEIVED;

    FacilityRequestDetailDataResponse data =
        new FacilityRequestDetailDataResponse(
            request.getId(),
            request.getReceiptNumber(),
            request.getTitle(),
            request.getDescription(),
            request.getEquipmentName(),
            new FacilityCategoryResponse(
                request.getFacilityCategory().getId(),
                request.getFacilityCategory().getName()
            ),
            new FacilityRequestLocationDetailResponse(
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

    return new FacilityRequestDetailResponse(data);
  }

  private boolean isPublic(FacilityRequest request) {
    return PUBLIC_VISIBILITY.equals(request.getVisibility());
  }

  private boolean canView(
      FacilityRequest request,
      Long facilityRequestId,
      Long viewerUserId,
      boolean owner,
      boolean administrator
  ) {
    return isPublic(request)
        || owner
        || administrator
        || isAssignedStaff(facilityRequestId, viewerUserId);
  }

  private boolean isAssignedStaff(
      Long facilityRequestId,
      Long viewerUserId
  ) {
    return viewerUserId != null
        && requestAssignmentRepository
        .existsByFacilityRequest_IdAndAssignedUser_IdAndEndedAtIsNull(
            facilityRequestId,
            viewerUserId
        );
  }

  private FacilityRequestAttachmentResponse toAttachmentResponse(
      FacilityRequestAttachment attachment
  ) {
    FileResource file = attachment.getFile();
    return new FacilityRequestAttachmentResponse(
        file.getId(),
        file.getOriginalFilename(),
        null
    );
  }

  private BusinessException notFound() {
    return new BusinessException(FacilityRequestErrorCode.NOT_FOUND);
  }
}
