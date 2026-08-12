package org.swbe.domain.lostitem.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.swbe.domain.lostitem.dto.request.StoredItemUpdateRequest;
import org.swbe.domain.lostitem.dto.response.StoredItemUpdateResponse;
import org.swbe.domain.lostitem.service.StoredItemUpdateService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequiredArgsConstructor
@Validated
public class StoredItemUpdateController {

  private final StoredItemUpdateService storedItemUpdateService;

  @PatchMapping(
      path = "/api/stored-items/{storedItemId}",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  public StoredItemUpdateResponse updateStoredItem(
      @PathVariable @Positive Long storedItemId,
      @Valid @RequestPart(name = "request", required = false)
      StoredItemUpdateRequest request,
      @RequestPart(name = "files", required = false)
      List<MultipartFile> files,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    boolean admin = principal.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(
            authority.getAuthority()
        ));
    return storedItemUpdateService.update(
        storedItemId,
        request,
        files == null ? List.of() : files,
        principal.getUserId(),
        admin
    );
  }
}
