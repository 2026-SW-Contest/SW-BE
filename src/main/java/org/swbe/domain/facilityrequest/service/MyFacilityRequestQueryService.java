package org.swbe.domain.facilityrequest.service;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListItemResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestPageResponse;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;

@Service
@Transactional(readOnly = true)
public class MyFacilityRequestQueryService {

  private static final Sort LATEST_FIRST = Sort.by(
      Sort.Order.desc("createdAt"),
      Sort.Order.desc("id")
  );

  private final FacilityRequestRepository facilityRequestRepository;
  private final FacilityRequestThumbnailService thumbnailService;

  public MyFacilityRequestQueryService(
      FacilityRequestRepository facilityRequestRepository,
      FacilityRequestThumbnailService thumbnailService
  ) {
    this.facilityRequestRepository = facilityRequestRepository;
    this.thumbnailService = thumbnailService;
  }

  // 로그인한 학생이 작성한 모든 시설 문의를 최신 등록순으로 조회한다.
  public FacilityRequestListResponse getMyFacilityRequests(
      Long requesterUserId,
      int page,
      int size
  ) {
    PageRequest pageable = PageRequest.of(page, size, LATEST_FIRST);
    Page<FacilityRequest> result = facilityRequestRepository
        .findAllByRequester_Id(requesterUserId, pageable);
    List<FacilityRequest> requests = result.getContent();
    Map<Long, String> thumbnailUrls = requests.isEmpty()
        ? Map.of()
        : thumbnailService.resolveAll(
            requests.stream().map(FacilityRequest::getId).toList()
        );
    List<FacilityRequestListItemResponse> content = requests.stream()
        .map(request -> toListItemResponse(
            request,
            thumbnailUrls.get(request.getId())
        ))
        .toList();
    FacilityRequestPageResponse data = new FacilityRequestPageResponse(
        content,
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages(),
        result.hasNext()
    );

    return new FacilityRequestListResponse(data);
  }

  // 시설 문의 엔터티를 마이페이지 목록에 필요한 간략한 정보로 변환한다.
  private FacilityRequestListItemResponse toListItemResponse(
      FacilityRequest request,
      String thumbnailUrl
  ) {
    FacilityRequestStatus status = FacilityRequestStatus.valueOf(
        request.getRequestStatus()
    );

    return new FacilityRequestListItemResponse(
        request.getId(),
        request.getTitle(),
        request.getFacilityCategory().getName(),
        request.getLocation().getName(),
        request.getRequestStatus(),
        status.getDisplayName(),
        thumbnailUrl,
        request.getCreatedAt()
    );
  }
}
