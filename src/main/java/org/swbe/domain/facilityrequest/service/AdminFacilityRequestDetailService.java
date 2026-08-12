package org.swbe.domain.facilityrequest.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.service.FilePublicUrlResolver;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestAdminResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestDetailDataResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestDetailResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestLocationResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestRequesterDetailResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityCategoryResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestAttachmentResponse;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestAttachment;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.entity.RequestComment;
import org.swbe.domain.facilityrequest.exception.FacilityRequestErrorCode;
import org.swbe.domain.facilityrequest.repository.FacilityRequestAttachmentRepository;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.domain.facilityrequest.repository.RequestCommentRepository;
import org.swbe.global.error.BusinessException;

@Service
@Transactional(readOnly = true)
public class AdminFacilityRequestDetailService {

  private static final String ADMIN_RESPONSE = "ADMIN_RESPONSE";

  private final FacilityRequestRepository facilityRequestRepository;
  private final FacilityRequestAttachmentRepository attachmentRepository;
  private final RequestCommentRepository requestCommentRepository;
  private final FilePublicUrlResolver filePublicUrlResolver;

  public AdminFacilityRequestDetailService(
      FacilityRequestRepository facilityRequestRepository,
      FacilityRequestAttachmentRepository attachmentRepository,
      RequestCommentRepository requestCommentRepository,
      FilePublicUrlResolver filePublicUrlResolver
  ) {
    this.facilityRequestRepository = facilityRequestRepository;
    this.attachmentRepository = attachmentRepository;
    this.requestCommentRepository = requestCommentRepository;
    this.filePublicUrlResolver = filePublicUrlResolver;
  }

  // 관리자가 선택한 시설문의의 작성자, 첨부사진, 답변 내역을 조회한다.
  public AdminFacilityRequestDetailResponse getFacilityRequest(
      Long facilityRequestId
  ) {
    FacilityRequest request = facilityRequestRepository
        .findAdminDetailById(facilityRequestId)
        .orElseThrow(() -> new BusinessException(
            FacilityRequestErrorCode.NOT_FOUND
        ));
    List<FacilityRequestAttachmentResponse> attachments =
        attachmentRepository
            .findAllByFacilityRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(
                facilityRequestId
            )
            .stream()
            .map(this::toAttachmentResponse)
            .toList();
    List<AdminFacilityRequestAdminResponse> adminResponses =
        requestCommentRepository
            .findAllByFacilityRequest_IdAndCommentTypeAndInternalFalseOrderByCreatedAtAscIdAsc(
                facilityRequestId,
                ADMIN_RESPONSE
            )
            .stream()
            .map(this::toAdminResponse)
            .toList();
    FacilityRequestStatus status = FacilityRequestStatus.valueOf(
        request.getRequestStatus()
    );
    AdminFacilityRequestDetailDataResponse data =
        new AdminFacilityRequestDetailDataResponse(
            request.getId(),
            request.getTitle(),
            request.getDescription(),
            new AdminFacilityRequestRequesterDetailResponse(
                request.getRequester().getId(),
                request.getRequester().getName(),
                request.getRequester().getStudentNumber(),
                request.getRequester().getEmail()
            ),
            new FacilityCategoryResponse(
                request.getFacilityCategory().getId(),
                request.getFacilityCategory().getName()
            ),
            new AdminFacilityRequestLocationResponse(
                request.getLocation().getId(),
                request.getLocation().getBuilding().getCode(),
                request.getLocation().getName()
            ),
            request.getRequestStatus(),
            status.getDisplayName(),
            attachments,
            adminResponses,
            request.getCreatedAt(),
            request.getUpdatedAt()
        );

    return new AdminFacilityRequestDetailResponse(data);
  }

  // 첨부파일 정보를 프론트에서 바로 조회할 수 있는 URL과 함께 변환한다.
  private FacilityRequestAttachmentResponse toAttachmentResponse(
      FacilityRequestAttachment attachment
  ) {
    FileResource file = attachment.getFile();
    return new FacilityRequestAttachmentResponse(
        file.getId(),
        file.getOriginalFilename(),
        filePublicUrlResolver.resolve(file)
    );
  }

  // 공개 관리자 답변 엔티티를 상세 응답 항목으로 변환한다.
  private AdminFacilityRequestAdminResponse toAdminResponse(
      RequestComment comment
  ) {
    return new AdminFacilityRequestAdminResponse(
        comment.getId(),
        comment.getContent(),
        comment.getCreatedAt()
    );
  }
}
