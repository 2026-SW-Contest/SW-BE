package org.swbe.domain.servicerequest.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.servicerequest.dto.request.ServiceRequestSearchCondition;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestDetailResponse;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestListResponse;
import org.swbe.domain.servicerequest.entity.ServiceRequestStatus;
import org.swbe.domain.servicerequest.service.ServiceRequestDetailService;
import org.swbe.domain.servicerequest.service.ServiceRequestQueryService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequestMapping("/api/service-requests")
@Validated
public class ServiceRequestController {

  private final ServiceRequestQueryService serviceRequestQueryService;
  private final ServiceRequestDetailService serviceRequestDetailService;

  public ServiceRequestController(
      ServiceRequestQueryService serviceRequestQueryService,
      ServiceRequestDetailService serviceRequestDetailService
  ) {
    this.serviceRequestQueryService = serviceRequestQueryService;
    this.serviceRequestDetailService = serviceRequestDetailService;
  }

  @GetMapping
  public ServiceRequestListResponse getServiceRequests(
      @RequestParam(required = false) @Positive Long categoryId,
      @RequestParam(required = false) @Positive Long locationId,
      @RequestParam(required = false) ServiceRequestStatus status,
      @RequestParam(required = false) @Size(max = 100) String keyword,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
  ) {
    return serviceRequestQueryService.getServiceRequests(
        new ServiceRequestSearchCondition(
            categoryId,
            locationId,
            status,
            keyword,
            from,
            to,
            page,
            size
        )
    );
  }

  @GetMapping("/{serviceRequestId}")
  public ServiceRequestDetailResponse getServiceRequest(
      @PathVariable @Positive Long serviceRequestId,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    Long viewerUserId = principal == null ? null : principal.getUserId();
    boolean administrator = principal != null
        && principal.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch("ROLE_ADMIN"::equals);

    return serviceRequestDetailService.getServiceRequest(
        serviceRequestId,
        viewerUserId,
        administrator
    );
  }
}
