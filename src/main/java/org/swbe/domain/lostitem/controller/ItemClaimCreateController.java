package org.swbe.domain.lostitem.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.swbe.domain.lostitem.dto.request.ItemClaimCreateRequest;
import org.swbe.domain.lostitem.dto.response.ItemClaimCreateResponse;
import org.swbe.domain.lostitem.service.ItemClaimCreateService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequiredArgsConstructor
@Validated
public class ItemClaimCreateController {

  private final ItemClaimCreateService itemClaimCreateService;

  @PostMapping(
      path = "/api/stored-items/{storedItemId}/claims",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  @ResponseStatus(HttpStatus.CREATED)
  public ItemClaimCreateResponse createItemClaim(
      @PathVariable @Positive Long storedItemId,
      @Valid @RequestPart("request") ItemClaimCreateRequest request,
      @RequestPart(name = "files", required = false)
      List<MultipartFile> files,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    return itemClaimCreateService.create(
        storedItemId,
        request,
        files == null ? List.of() : files,
        principal.getUserId()
    );
  }
}
