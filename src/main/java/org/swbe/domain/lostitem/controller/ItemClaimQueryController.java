package org.swbe.domain.lostitem.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.lostitem.dto.request.ItemClaimSearchCondition;
import org.swbe.domain.lostitem.dto.response.ItemClaimDetailResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimListResponse;
import org.swbe.domain.lostitem.dto.response.OfficeItemClaimListResponse;
import org.swbe.domain.lostitem.entity.ItemClaimStatus;
import org.swbe.domain.lostitem.service.ItemClaimQueryService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequiredArgsConstructor
@Validated
public class ItemClaimQueryController {

  private final ItemClaimQueryService itemClaimQueryService;

  @GetMapping("/api/stored-items/{storedItemId}/claims")
  public ItemClaimListResponse getItemClaims(
      @PathVariable @Positive Long storedItemId,
      @RequestParam(required = false) ItemClaimStatus status,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    return itemClaimQueryService.getItemClaims(
        storedItemId,
        new ItemClaimSearchCondition(status, page, size),
        principal.getUserId(),
        isAdmin(principal)
    );
  }

  @GetMapping("/api/lost-item-offices/{officeId}/claims")
  public OfficeItemClaimListResponse getOfficeItemClaims(
      @PathVariable @Positive Long officeId,
      @RequestParam(required = false) ItemClaimStatus status,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    return itemClaimQueryService.getOfficeItemClaims(
        officeId,
        new ItemClaimSearchCondition(status, page, size),
        principal.getUserId(),
        isAdmin(principal)
    );
  }

  @GetMapping("/api/item-claims/{itemClaimId}")
  public ItemClaimDetailResponse getItemClaim(
      @PathVariable @Positive Long itemClaimId,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    return itemClaimQueryService.getItemClaim(
        itemClaimId,
        principal.getUserId(),
        isAdmin(principal)
    );
  }

  private boolean isAdmin(AppUserPrincipal principal) {
    return principal.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(
            authority.getAuthority()
        ));
  }
}
