package org.swbe.domain.lostitem.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.lostitem.dto.response.LostItemOfficeListResponse;
import org.swbe.domain.lostitem.service.LostItemOfficeQueryService;

@RestController
@RequestMapping("/api/lost-item-offices")
@RequiredArgsConstructor
public class LostItemOfficeController {

  private final LostItemOfficeQueryService officeQueryService;

  @GetMapping
  public LostItemOfficeListResponse getOffices() {
    return officeQueryService.getOffices();
  }
}
