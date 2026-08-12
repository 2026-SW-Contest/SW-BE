package org.swbe.domain.lostitem.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.lostitem.service.StoredItemDeleteService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequiredArgsConstructor
@Validated
public class StoredItemDeleteController {

  private final StoredItemDeleteService storedItemDeleteService;

  @DeleteMapping("/api/stored-items/{storedItemId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteStoredItem(
      @PathVariable @Positive Long storedItemId,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    boolean admin = principal.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(
            authority.getAuthority()
        ));
    storedItemDeleteService.delete(
        storedItemId,
        principal.getUserId(),
        admin
    );
  }
}
