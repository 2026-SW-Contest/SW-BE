package org.swbe.domain.lostitem.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.lostitem.dto.request.ItemClaimDecisionRequest;
import org.swbe.domain.lostitem.dto.response.ItemClaimDecisionResponse;
import org.swbe.domain.lostitem.service.ItemClaimDecisionService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequiredArgsConstructor
@Validated
public class ItemClaimDecisionController {

  private final ItemClaimDecisionService decisionService;

  @PatchMapping("/api/item-claims/{itemClaimId}/decision")
  public ItemClaimDecisionResponse decide(
      @PathVariable @Positive Long itemClaimId,
      @Valid @RequestBody ItemClaimDecisionRequest request,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    boolean admin = principal.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(
            authority.getAuthority()
        ));
    return decisionService.decide(
        itemClaimId,
        request,
        principal.getUserId(),
        admin
    );
  }
}
