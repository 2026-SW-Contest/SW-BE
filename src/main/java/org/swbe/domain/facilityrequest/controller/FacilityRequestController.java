package org.swbe.domain.facilityrequest.controller;

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
import org.swbe.domain.facilityrequest.dto.request.FacilityRequestSearchCondition;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestDetailResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListResponse;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.service.FacilityRequestDetailService;
import org.swbe.domain.facilityrequest.service.FacilityRequestQueryService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequestMapping("/api/facility-requests")
@Validated
public class FacilityRequestController {

  private final FacilityRequestQueryService facilityRequestQueryService;
  private final FacilityRequestDetailService facilityRequestDetailService;

  public FacilityRequestController(
      FacilityRequestQueryService facilityRequestQueryService,
      FacilityRequestDetailService facilityRequestDetailService
  ) {
    this.facilityRequestQueryService = facilityRequestQueryService;
    this.facilityRequestDetailService = facilityRequestDetailService;
  }

  @GetMapping
  public FacilityRequestListResponse getFacilityRequests(
      @RequestParam(required = false) @Positive Long categoryId,
      @RequestParam(required = false) @Positive Long locationId,
      @RequestParam(required = false) FacilityRequestStatus status,
      @RequestParam(required = false) @Size(max = 100) String keyword,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
  ) {
    return facilityRequestQueryService.getFacilityRequests(
        new FacilityRequestSearchCondition(
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

  @GetMapping("/{facilityRequestId}")
  public FacilityRequestDetailResponse getFacilityRequest(
      @PathVariable @Positive Long facilityRequestId,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    Long viewerUserId = principal == null ? null : principal.getUserId();
    boolean administrator = principal != null
        && principal.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch("ROLE_ADMIN"::equals);

    return facilityRequestDetailService.getFacilityRequest(
        facilityRequestId,
        viewerUserId,
        administrator
    );
  }
}
