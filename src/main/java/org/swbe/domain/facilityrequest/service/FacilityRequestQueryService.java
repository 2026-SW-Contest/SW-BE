package org.swbe.domain.facilityrequest.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.facilityrequest.dto.request.FacilityRequestSearchCondition;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListItemResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestPageResponse;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.global.error.BusinessException;
import org.swbe.global.error.CommonErrorCode;

@Service
@Transactional(readOnly = true)
public class FacilityRequestQueryService {

  private static final Sort DEFAULT_SORT = Sort.by(
      Sort.Order.desc("createdAt"),
      Sort.Order.desc("id")
  );

  private final FacilityRequestRepository facilityRequestRepository;
  private final FacilityRequestThumbnailService thumbnailService;

  public FacilityRequestQueryService(
      FacilityRequestRepository facilityRequestRepository,
      FacilityRequestThumbnailService thumbnailService
  ) {
    this.facilityRequestRepository = facilityRequestRepository;
    this.thumbnailService = thumbnailService;
  }

  public FacilityRequestListResponse getFacilityRequests(
      FacilityRequestSearchCondition condition
  ) {
    validateDateRange(condition);

    LocalDateTime fromDateTime = condition.from() == null
        ? null
        : condition.from().atStartOfDay();
    LocalDateTime toDateTimeExclusive = condition.to() == null
        ? null
        : condition.to().plusDays(1).atStartOfDay();
    String status = condition.status() == null
        ? null
        : condition.status().name();
    PageRequest pageable = PageRequest.of(
        condition.page(),
        condition.size(),
        DEFAULT_SORT
    );

    Page<FacilityRequest> result = facilityRequestRepository.searchRequests(
        condition.categoryId(),
        condition.locationId(),
        status,
        condition.keyword(),
        fromDateTime,
        toDateTimeExclusive,
        pageable
    );
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
    FacilityRequestPageResponse page = new FacilityRequestPageResponse(
        content,
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages(),
        result.hasNext()
    );

    return new FacilityRequestListResponse(page);
  }

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

  private void validateDateRange(FacilityRequestSearchCondition condition) {
    if (condition.from() != null
        && condition.to() != null
        && condition.from().isAfter(condition.to())) {
      throw new BusinessException(CommonErrorCode.VALIDATION_FAILED);
    }
  }
}
