package org.swbe.domain.facilityrequest.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListResponse;
import org.swbe.domain.facilityrequest.service.MyFacilityRequestQueryService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequestMapping("/api/users/me/facility-requests")
@Validated
public class MyFacilityRequestController {

  private final MyFacilityRequestQueryService myFacilityRequestQueryService;

  public MyFacilityRequestController(
      MyFacilityRequestQueryService myFacilityRequestQueryService
  ) {
    this.myFacilityRequestQueryService = myFacilityRequestQueryService;
  }

  // 로그인한 학생이 작성한 시설 문의 목록을 조회한다.
  @GetMapping
  public FacilityRequestListResponse getMyFacilityRequests(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    return myFacilityRequestQueryService.getMyFacilityRequests(
        principal.getUserId(),
        page,
        size
    );
  }
}
