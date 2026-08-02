package org.swbe.domain.servicerequest.service;

import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.servicerequest.dto.request.ServiceRequestSearchCondition;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestListItemResponse;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestListResponse;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestPageResponse;
import org.swbe.domain.servicerequest.entity.ServiceRequest;
import org.swbe.domain.servicerequest.entity.ServiceRequestStatus;
import org.swbe.domain.servicerequest.repository.ServiceRequestRepository;
import org.swbe.global.error.BusinessException;
import org.swbe.global.error.CommonErrorCode;

@Service
@Transactional(readOnly = true)
public class ServiceRequestQueryService {

  private static final Sort DEFAULT_SORT = Sort.by(
      Sort.Order.desc("createdAt"),
      Sort.Order.desc("id")
  );

  private final ServiceRequestRepository serviceRequestRepository;

  public ServiceRequestQueryService(
      ServiceRequestRepository serviceRequestRepository
  ) {
    this.serviceRequestRepository = serviceRequestRepository;
  }

  public ServiceRequestListResponse getServiceRequests(
      ServiceRequestSearchCondition condition
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

    var result = serviceRequestRepository.searchPublicRequests(
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
    var page = new ServiceRequestPageResponse(
        content,
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages(),
        result.hasNext()
    );

    return new ServiceRequestListResponse(page);
  }

  private ServiceRequestListItemResponse toListItemResponse(
      ServiceRequest request
  ) {
    ServiceRequestStatus status = ServiceRequestStatus.valueOf(
        request.getRequestStatus()
    );

    return new ServiceRequestListItemResponse(
        request.getId(),
        request.getTitle(),
        request.getRequestCategory().getName(),
        request.getLocation().getName(),
        request.getRequestStatus(),
        status.getDisplayName(),
        null,
        request.getCreatedAt()
    );
  }

  private void validateDateRange(ServiceRequestSearchCondition condition) {
    if (condition.from() != null
        && condition.to() != null
        && condition.from().isAfter(condition.to())) {
      throw new BusinessException(CommonErrorCode.VALIDATION_FAILED);
    }
  }
}
