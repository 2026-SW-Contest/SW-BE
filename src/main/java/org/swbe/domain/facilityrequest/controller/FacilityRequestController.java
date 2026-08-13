package org.swbe.domain.facilityrequest.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import org.swbe.domain.facilityrequest.dto.request.FacilityRequestUpdateRequest;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestCreateResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestDetailResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestUpdateResponse;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.service.FacilityRequestCreateService;
import org.swbe.domain.facilityrequest.service.FacilityRequestDeleteService;
import org.swbe.domain.facilityrequest.service.FacilityRequestDetailService;
import org.swbe.domain.facilityrequest.service.FacilityRequestQueryService;
import org.swbe.domain.facilityrequest.service.FacilityRequestUpdateService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequestMapping("/api/facility-requests")
@Validated
public class FacilityRequestController {

  private final FacilityRequestQueryService facilityRequestQueryService;
  private final FacilityRequestDetailService facilityRequestDetailService;
  private final FacilityRequestCreateService facilityRequestCreateService;

  private final FacilityRequestDeleteService facilityRequestDeleteService;
  private final FacilityRequestUpdateService facilityRequestUpdateService;

  public FacilityRequestController(
      FacilityRequestQueryService facilityRequestQueryService,
      FacilityRequestDetailService facilityRequestDetailService,
      FacilityRequestCreateService facilityRequestCreateService,
      FacilityRequestDeleteService facilityRequestDeleteService,
      FacilityRequestUpdateService facilityRequestUpdateService
  ) {
    this.facilityRequestQueryService = facilityRequestQueryService;
    this.facilityRequestDetailService = facilityRequestDetailService;
    this.facilityRequestCreateService = facilityRequestCreateService;
    this.facilityRequestDeleteService = facilityRequestDeleteService;
    this.facilityRequestUpdateService = facilityRequestUpdateService;
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
      @RequestParam(required = false) @Size(max = 512) String cursor,
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
            cursor,
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

  // 로그인한 학생이 작성한 대기 상태의 문의를 삭제한다.
  @DeleteMapping("/{facilityRequestId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteFacilityRequest(
      @PathVariable @Positive Long facilityRequestId,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    facilityRequestDeleteService.delete(
        facilityRequestId,
        principal.getUserId()
    );
  }

  // 로그인한 학생이 작성한 대기 상태의 문의를 부분 수정한다.
  @PatchMapping(
      value = "/{facilityRequestId}",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  public FacilityRequestUpdateResponse updateFacilityRequest(
      @PathVariable @Positive Long facilityRequestId,
      @Valid @RequestPart(name = "request", required = false)
      FacilityRequestUpdateRequest request,
      @RequestPart(name = "files", required = false)
      List<MultipartFile> files,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    return facilityRequestUpdateService.update(
        facilityRequestId,
        request,
        files,
        principal.getUserId()
    );
  }
}
