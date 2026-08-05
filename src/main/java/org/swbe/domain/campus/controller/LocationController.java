package org.swbe.domain.campus.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.campus.dto.response.LocationListResponse;
import org.swbe.domain.campus.service.LocationQueryService;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

  private final LocationQueryService locationQueryService;

  @GetMapping
  public LocationListResponse getLocations() {
    return locationQueryService.getLocations();
  }
}
