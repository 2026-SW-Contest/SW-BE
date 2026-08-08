package org.swbe.domain.search.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.search.dto.response.FacilityRequestSearchResponse;
import org.swbe.domain.search.dto.response.LostItemSearchResponse;
import org.swbe.domain.search.dto.response.SearchSummaryResponse;
import org.swbe.domain.search.service.IntegratedSearchService;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Validated
public class SearchController {

  private final IntegratedSearchService integratedSearchService;

  @GetMapping("/summary")
  public SearchSummaryResponse getSummary(
      @RequestParam @NotBlank @Size(max = 100) String keyword
  ) {
    return integratedSearchService.getSummary(keyword);
  }

  @GetMapping("/lost-items")
  public LostItemSearchResponse searchLostItems(
      @RequestParam @NotBlank @Size(max = 100) String keyword,
      @RequestParam(required = false)
      @Size(max = 512) String cursor,
      @RequestParam(defaultValue = "20")
      @Min(1) @Max(50) int size
  ) {
    return integratedSearchService.searchLostItems(
        keyword,
        cursor,
        size
    );
  }

  @GetMapping("/facility-requests")
  public FacilityRequestSearchResponse searchFacilityRequests(
      @RequestParam @NotBlank @Size(max = 100) String keyword,
      @RequestParam(required = false)
      @Size(max = 512) String cursor,
      @RequestParam(defaultValue = "20")
      @Min(1) @Max(50) int size
  ) {
    return integratedSearchService.searchFacilityRequests(
        keyword,
        cursor,
        size
    );
  }
}
