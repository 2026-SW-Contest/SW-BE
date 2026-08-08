package org.swbe.domain.search.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.swbe.domain.search.dto.response.SearchSummaryDataResponse;
import org.swbe.domain.search.dto.response.SearchSummaryResponse;
import org.swbe.domain.search.dto.response.SearchSuggestionListResponse;
import org.swbe.domain.search.service.IntegratedSearchService;
import org.swbe.domain.search.service.SearchSuggestionService;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(SearchController.class)
@Import({
    SecurityConfig.class,
    SecurityErrorResponseWriter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    RestSessionInformationExpiredStrategy.class
})
@TestPropertySource(properties = {
    "app.security.frontend-origins[0]=http://localhost:3000"
})
class SearchControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private IntegratedSearchService integratedSearchService;

  @MockitoBean
  private SearchSuggestionService searchSuggestionService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void anonymousUserCanGetSearchSuggestions() throws Exception {
    when(searchSuggestionService.getSuggestions("에어", 8))
        .thenReturn(new SearchSuggestionListResponse(List.of(
            "에어팟 프로",
            "천장형 에어컨"
        )));

    mockMvc.perform(get("/api/search/suggestions")
            .queryParam("query", "에어"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0]").value("에어팟 프로"))
        .andExpect(jsonPath("$.data[1]").value("천장형 에어컨"));
  }

  @Test
  void oversizedSuggestionLimitIsRejected() throws Exception {
    mockMvc.perform(get("/api/search/suggestions")
            .queryParam("query", "에어")
            .queryParam("size", "21"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code")
            .value("COMMON_VALIDATION_FAILED"));
  }

  @Test
  void anonymousUserCanGetIntegratedSearchSummary() throws Exception {
    when(integratedSearchService.getSummary("에어")).thenReturn(
        new SearchSummaryResponse(
            new SearchSummaryDataResponse("에어", 12L, 4L)
        )
    );

    mockMvc.perform(get("/api/search/summary")
            .queryParam("keyword", "에어"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.keyword").value("에어"))
        .andExpect(jsonPath("$.data.lostItemCount").value(12))
        .andExpect(jsonPath("$.data.facilityRequestCount").value(4));
  }

  @Test
  void blankKeywordIsRejected() throws Exception {
    mockMvc.perform(get("/api/search/summary")
            .queryParam("keyword", " "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code")
            .value("COMMON_VALIDATION_FAILED"));
  }

  @Test
  void oversizedSearchSliceIsRejected() throws Exception {
    mockMvc.perform(get("/api/search/lost-items")
            .queryParam("keyword", "에어")
            .queryParam("size", "51"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code")
            .value("COMMON_VALIDATION_FAILED"));
  }
}
