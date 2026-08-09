package org.swbe.domain.search.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.search.dto.request.RecentSearchCreateRequest;
import org.swbe.domain.search.dto.response.RecentSearchListResponse;
import org.swbe.domain.search.service.RecentSearchService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequestMapping("/api/recent-searches")
@RequiredArgsConstructor
@Validated
public class RecentSearchController {

  private final RecentSearchService recentSearchService;

  @GetMapping
  public RecentSearchListResponse getRecentSearches(
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    return recentSearchService.getRecentSearches(
        principal.getUserId()
    );
  }

  @PostMapping
  public RecentSearchListResponse record(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @Valid @RequestBody RecentSearchCreateRequest request
  ) {
    return recentSearchService.record(
        principal.getUserId(),
        request
    );
  }

  @DeleteMapping("/{recentSearchId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable @Positive Long recentSearchId
  ) {
    recentSearchService.delete(
        principal.getUserId(),
        recentSearchId
    );
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteAll(
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    recentSearchService.deleteAll(principal.getUserId());
  }
}
