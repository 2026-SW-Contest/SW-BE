package org.swbe.domain.lostitem.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.lostitem.dto.request.StoredItemSearchCondition;
import org.swbe.domain.lostitem.dto.response.StoredItemDetailResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemListResponse;
import org.swbe.domain.lostitem.entity.StoredItemStatus;
import org.swbe.domain.lostitem.service.StoredItemDetailService;
import org.swbe.domain.lostitem.service.StoredItemQueryService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequestMapping("/api/stored-items")
@RequiredArgsConstructor
@Validated
public class StoredItemController {

  private final StoredItemQueryService storedItemQueryService;
  private final StoredItemDetailService storedItemDetailService;

  @GetMapping
  public StoredItemListResponse getStoredItems(
      @RequestParam(required = false) @Positive Long categoryId,
      @RequestParam(required = false) @Positive Long locationId,
      @RequestParam(required = false) StoredItemStatus status,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) @Size(max = 512) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
  ) {
    return storedItemQueryService.getStoredItems(
        new StoredItemSearchCondition(
            categoryId,
            locationId,
            status,
            from,
            to,
            cursor,
            size
        )
    );
  }

  @GetMapping("/{storedItemId}")
  public StoredItemDetailResponse getStoredItem(
      @PathVariable @Positive Long storedItemId,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    return storedItemDetailService.getStoredItem(
        storedItemId,
        principal == null ? null : principal.getUserId()
    );
  }
}
