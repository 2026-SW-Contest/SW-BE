package org.swbe.domain.facilityrequest.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.service.FilePublicUrlResolver;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestAttachmentResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestAdminResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityCategoryResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestDetailDataResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestDetailResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestLocationDetailResponse;
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
public class FacilityRequestDetailService {

  private static final String ADMIN_RESPONSE = "ADMIN_RESPONSE";

  private final FacilityRequestRepository facilityRequestRepository;
  private final FacilityRequestAttachmentRepository attachmentRepository;
  private final RequestCommentRepository requestCommentRepository;
  private final FilePublicUrlResolver filePublicUrlResolver;

  public FacilityRequestDetailService(
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

  // 시설 문의의 내용과 첨부사진, 소유 여부 및 공개 관리자 답변을 조회한다.
  public FacilityRequestDetailResponse getFacilityRequest(
      Long facilityRequestId,
      Long viewerUserId
  ) {
    FacilityRequest request = facilityRequestRepository
        .findDetailById(facilityRequestId)
        .orElseThrow(this::notFound);
    boolean ownedByCurrentUser = request.isRequestedBy(viewerUserId);

    List<FacilityRequestAttachmentResponse> attachments = attachmentRepository
        .findAllByFacilityRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(
            facilityRequestId
        )
        .stream()
        .map(this::toAttachmentResponse)
        .toList();
    List<FacilityRequestAdminResponse> adminResponses =
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
    boolean editable = ownedByCurrentUser && request.isEditable();
    boolean deletable = ownedByCurrentUser && request.isDeletable();

    FacilityRequestDetailDataResponse data =
        new FacilityRequestDetailDataResponse(
            request.getId(),
            request.getTitle(),
            request.getDescription(),
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
            ownedByCurrentUser,
            editable,
            deletable,
            adminResponses,
            request.getCreatedAt(),
            request.getUpdatedAt()
        );

    return new FacilityRequestDetailResponse(data);
  }

  // 공개 관리자 답변 엔터티를 상세 조회 응답 항목으로 변환한다.
  private FacilityRequestAdminResponse toAdminResponse(
      RequestComment comment
  ) {
    return new FacilityRequestAdminResponse(
        comment.getId(),
        comment.getContent(),
        comment.getCreatedAt()
    );
  }

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

  private BusinessException notFound() {
    return new BusinessException(FacilityRequestErrorCode.NOT_FOUND);
  }
}
