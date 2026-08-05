package org.swbe.domain.facilityrequest.service;

import java.time.LocalDateTime;
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

  public FacilityRequestQueryService(
      FacilityRequestRepository facilityRequestRepository
  ) {
    this.facilityRequestRepository = facilityRequestRepository;
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

    var result = facilityRequestRepository.searchPublicRequests(
        condition.categoryId(),
        condition.locationId(),
        status,
        condition.keyword(),
        fromDateTime,
        toDateTimeExclusive,
        pageable
    );
    var content = result.getContent().stream()
        .map(this::toListItemResponse)
        .toList();
    var page = new FacilityRequestPageResponse(
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
      FacilityRequest request
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
        null,
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
