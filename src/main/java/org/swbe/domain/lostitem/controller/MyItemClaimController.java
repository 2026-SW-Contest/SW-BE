package org.swbe.domain.lostitem.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.lostitem.dto.response.MyItemClaimListResponse;
import org.swbe.domain.lostitem.service.MyItemClaimQueryService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequestMapping("/api/users/me/item-claims")
@RequiredArgsConstructor
@Validated
public class MyItemClaimController {

  private final MyItemClaimQueryService myItemClaimQueryService;

  // 로그인한 학생이 등록한 소유자 확인 요청 목록을 조회한다.
  @GetMapping
  public MyItemClaimListResponse getMyItemClaims(
      @RequestParam(required = false) @Size(max = 512) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    return myItemClaimQueryService.getMyItemClaims(
        principal.getUserId(),
        cursor,
        size
    );
  }
}
