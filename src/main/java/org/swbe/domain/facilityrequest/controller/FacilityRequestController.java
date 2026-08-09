package org.swbe.domain.facilityrequest.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.swbe.domain.facilityrequest.dto.request.FacilityRequestCreateRequest;
import org.swbe.domain.facilityrequest.dto.request.FacilityRequestSearchCondition;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestCreateResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestDetailResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListResponse;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.service.FacilityRequestCreateService;
import org.swbe.domain.facilityrequest.service.FacilityRequestCancelService;
import org.swbe.domain.facilityrequest.service.FacilityRequestDetailService;
import org.swbe.domain.facilityrequest.service.FacilityRequestQueryService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequestMapping("/api/facility-requests")
@Validated
public class FacilityRequestController {

  private final FacilityRequestQueryService facilityRequestQueryService;
  private final FacilityRequestDetailService facilityRequestDetailService;
  private final FacilityRequestCreateService facilityRequestCreateService;
  private final FacilityRequestCancelService facilityRequestCancelService;

  public FacilityRequestController(
      FacilityRequestQueryService facilityRequestQueryService,
      FacilityRequestDetailService facilityRequestDetailService,
      FacilityRequestCreateService facilityRequestCreateService,
      FacilityRequestCancelService facilityRequestCancelService
  ) {
    this.facilityRequestQueryService = facilityRequestQueryService;
    this.facilityRequestDetailService = facilityRequestDetailService;
    this.facilityRequestCreateService = facilityRequestCreateService;
    this.facilityRequestCancelService = facilityRequestCancelService;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public FacilityRequestCreateResponse createFacilityRequest(
      @Valid @RequestPart("request") FacilityRequestCreateRequest request,
      @RequestPart(name = "files", required = false)
      List<MultipartFile> files,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    return facilityRequestCreateService.create(
        request,
        files == null ? List.of() : files,
        principal.getUserId()
    );
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
    return facilityRequestDetailService.getFacilityRequest(
        facilityRequestId,
        viewerUserId
    );
  }

  // 로그인한 학생이 작성한 접수 상태의 문의를 취소한다.
  @DeleteMapping("/{facilityRequestId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelFacilityRequest(
      @PathVariable @Positive Long facilityRequestId,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    facilityRequestCancelService.cancel(
        facilityRequestId,
        principal.getUserId()
    );
  }
}
