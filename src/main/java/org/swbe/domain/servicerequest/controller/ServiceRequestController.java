package org.swbe.domain.servicerequest.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.servicerequest.dto.request.ServiceRequestSearchCondition;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestListResponse;
import org.swbe.domain.servicerequest.entity.ServiceRequestStatus;
import org.swbe.domain.servicerequest.service.ServiceRequestQueryService;

@RestController
@RequestMapping("/api/service-requests")
@Validated
public class ServiceRequestController {

  private final ServiceRequestQueryService serviceRequestQueryService;

  public ServiceRequestController(
      ServiceRequestQueryService serviceRequestQueryService
  ) {
    this.serviceRequestQueryService = serviceRequestQueryService;
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
}
