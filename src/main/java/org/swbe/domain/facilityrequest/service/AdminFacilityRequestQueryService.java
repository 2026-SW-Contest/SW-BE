package org.swbe.domain.facilityrequest.service;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.facilityrequest.dto.request.AdminFacilityRequestSearchCondition;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestListItemResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestListResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestLocationResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestPageResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestRequesterResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityCategoryResponse;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.global.error.BusinessException;
import org.swbe.global.error.CommonErrorCode;

@Service
@Transactional(readOnly = true)
public class AdminFacilityRequestQueryService {

  private final FacilityRequestRepository facilityRequestRepository;
  private final FacilityRequestThumbnailService thumbnailService;

  public AdminFacilityRequestQueryService(
      FacilityRequestRepository facilityRequestRepository,
      FacilityRequestThumbnailService thumbnailService
  ) {
    this.facilityRequestRepository = facilityRequestRepository;
    this.thumbnailService = thumbnailService;
  }

  // 관리자 검색 조건으로 시설문의를 조회하고 관리용 목록 응답을 생성한다.
  public AdminFacilityRequestListResponse getFacilityRequests(
      AdminFacilityRequestSearchCondition condition
  ) {
    validateCondition(condition);
    PageRequest pageable = PageRequest.of(
        condition.page(),
        condition.size()
    );
    Page<FacilityRequest> result =
        facilityRequestRepository.searchAdminRequests(
            condition,
            pageable
        );
    List<FacilityRequest> requests = result.getContent();
    Map<Long, String> thumbnailUrls = requests.isEmpty()
        ? Map.of()
        : thumbnailService.resolveAll(
            requests.stream()
                .map(FacilityRequest::getId)
                .toList()
        );
    List<AdminFacilityRequestListItemResponse> content = requests.stream()
        .map(request -> toListItemResponse(
            request,
            thumbnailUrls.get(request.getId())
        ))
        .toList();
    AdminFacilityRequestPageResponse data =
        new AdminFacilityRequestPageResponse(
            content,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.hasNext()
        );

    return new AdminFacilityRequestListResponse(data);
  }

  // 날짜 범위와 관리자 화면에서 허용하는 상태 필터를 검증한다.
  private void validateCondition(
      AdminFacilityRequestSearchCondition condition
  ) {
    if (condition.from() != null
        && condition.to() != null
        && condition.from().isAfter(condition.to())) {
      throw new BusinessException(CommonErrorCode.VALIDATION_FAILED);
    }
  }

  // 조회된 엔티티를 작성자 정보가 포함된 관리자 목록 항목으로 변환한다.
  private AdminFacilityRequestListItemResponse toListItemResponse(
      FacilityRequest request,
      String thumbnailUrl
  ) {
    FacilityRequestStatus status = FacilityRequestStatus.valueOf(
        request.getRequestStatus()
    );
    AdminFacilityRequestRequesterResponse requester =
        new AdminFacilityRequestRequesterResponse(
            request.getRequester().getId(),
            request.getRequester().getName(),
            request.getRequester().getStudentNumber()
        );
    FacilityCategoryResponse category = new FacilityCategoryResponse(
        request.getFacilityCategory().getId(),
        request.getFacilityCategory().getName()
    );
    AdminFacilityRequestLocationResponse location =
        new AdminFacilityRequestLocationResponse(
            request.getLocation().getId(),
            request.getLocation().getBuilding().getCode(),
            request.getLocation().getName()
        );

    return new AdminFacilityRequestListItemResponse(
        request.getId(),
        request.getTitle(),
        requester,
        category,
        location,
        request.getRequestStatus(),
        status.getDisplayName(),
        thumbnailUrl,
        request.getCreatedAt()
    );
  }
}
