package org.swbe.domain.lostitem.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.swbe.domain.lostitem.dto.request.StoredItemCreateRequest;
import org.swbe.domain.lostitem.dto.response.StoredItemCreateResponse;
import org.swbe.domain.lostitem.service.StoredItemCreateService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequiredArgsConstructor
public class StoredItemCreateController {

  private final StoredItemCreateService storedItemCreateService;

  @PostMapping(
      path = "/api/lost-item",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  @ResponseStatus(HttpStatus.CREATED)
  public StoredItemCreateResponse createStoredItem(
      @Valid @RequestPart("request") StoredItemCreateRequest request,
      @RequestPart(name = "files", required = false)
      List<MultipartFile> files,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    boolean admin = principal.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(
            authority.getAuthority()
        ));
    return storedItemCreateService.create(
        request,
        files == null ? List.of() : files,
        principal.getUserId(),
        admin
    );
  }
}
