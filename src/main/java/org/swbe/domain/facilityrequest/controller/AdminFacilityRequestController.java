package org.swbe.domain.facilityrequest.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.facilityrequest.dto.request.AdminFacilityRequestSearchCondition;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestListResponse;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.service.AdminFacilityRequestQueryService;

@RestController
@RequestMapping("/api/admin/facility-requests")
@RequiredArgsConstructor
@Validated
public class AdminFacilityRequestController {

  private final AdminFacilityRequestQueryService queryService;

  // 관리자가 시설문의 목록을 검색 조건과 함께 조회한다.
  @GetMapping
  public AdminFacilityRequestListResponse getFacilityRequests(
      @RequestParam(required = false) @Size(max = 100) String keyword,
      @RequestParam(required = false) FacilityRequestStatus status,
      @RequestParam(required = false) @Positive Long categoryId,
      @RequestParam(required = false) @Positive Long locationId,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
  ) {
    AdminFacilityRequestSearchCondition condition =
        new AdminFacilityRequestSearchCondition(
            keyword,
            status,
            categoryId,
            locationId,
            from,
            to,
            page,
            size
        );

    return queryService.getFacilityRequests(condition);
  }
}
