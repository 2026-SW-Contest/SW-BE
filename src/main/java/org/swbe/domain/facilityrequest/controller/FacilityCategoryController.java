package org.swbe.domain.facilityrequest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.facilityrequest.dto.response.FacilityCategoryListResponse;
import org.swbe.domain.facilityrequest.service.FacilityCategoryQueryService;

@RestController
@RequestMapping("/api/facility-categories")
@RequiredArgsConstructor
public class FacilityCategoryController {

  private final FacilityCategoryQueryService facilityCategoryQueryService;

  @GetMapping
  public FacilityCategoryListResponse getCategories() {
    return facilityCategoryQueryService.getCategories();
  }
}
