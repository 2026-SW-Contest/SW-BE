package org.swbe.domain.servicerequest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestCategoryListResponse;
import org.swbe.domain.servicerequest.service.RequestCategoryQueryService;

@RestController
@RequestMapping("/api/request-categories")
@RequiredArgsConstructor
public class ServiceRequestCategoryController {

  private final RequestCategoryQueryService requestCategoryQueryService;

  @GetMapping
  public ServiceRequestCategoryListResponse getCategories() {
    return requestCategoryQueryService.getCategories();
  }
}
