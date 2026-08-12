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
import org.swbe.domain.lostitem.dto.request.StoredItemStatusUpdateRequest;
import org.swbe.domain.lostitem.dto.response.StoredItemStatusUpdateResponse;
import org.swbe.domain.lostitem.service.StoredItemStatusUpdateService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequiredArgsConstructor
@Validated
public class StoredItemStatusController {

  private final StoredItemStatusUpdateService statusUpdateService;

  @PatchMapping("/api/stored-items/{storedItemId}/status")
  public StoredItemStatusUpdateResponse updateStatus(
      @PathVariable @Positive Long storedItemId,
      @Valid @RequestBody StoredItemStatusUpdateRequest request,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    boolean admin = principal.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(
            authority.getAuthority()
        ));
    return statusUpdateService.updateStatus(
        storedItemId,
        request,
        principal.getUserId(),
        admin
    );
  }
}
