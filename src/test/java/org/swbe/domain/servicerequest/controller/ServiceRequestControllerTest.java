package org.swbe.domain.servicerequest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestListItemResponse;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestListResponse;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestPageResponse;
import org.swbe.domain.servicerequest.service.ServiceRequestQueryService;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(ServiceRequestController.class)
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
class ServiceRequestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ServiceRequestQueryService serviceRequestQueryService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void anonymousUserCanGetFilteredServiceRequestList() throws Exception {
    var item = new ServiceRequestListItemResponse(
        25L,
        "학생회관 1층 조명 깜빡임",
        "전기/조명",
        "학생회관",
        "IN_PROGRESS",
        "진행중",
        null,
        LocalDateTime.of(2026, 8, 1, 16, 0)
    );
    when(serviceRequestQueryService.getServiceRequests(any())).thenReturn(
        new ServiceRequestListResponse(
            new ServiceRequestPageResponse(
                List.of(item),
                0,
                20,
                1,
                1,
                false
            )
        )
    );

    mockMvc.perform(get("/api/service-requests")
            .queryParam("categoryId", "1")
            .queryParam("locationId", "2")
            .queryParam("status", "IN_PROGRESS")
            .queryParam("keyword", "조명")
            .queryParam("from", "2026-07-01")
            .queryParam("to", "2026-08-01")
            .queryParam("page", "0")
            .queryParam("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].serviceRequestId").value(25))
        .andExpect(jsonPath("$.data.content[0].title")
            .value("학생회관 1층 조명 깜빡임"))
        .andExpect(jsonPath("$.data.content[0].categoryName")
            .value("전기/조명"))
        .andExpect(jsonPath("$.data.content[0].locationName")
            .value("학생회관"))
        .andExpect(jsonPath("$.data.content[0].requestStatus")
            .value("IN_PROGRESS"))
        .andExpect(jsonPath("$.data.content[0].requestStatusName")
            .value("진행중"))
        .andExpect(jsonPath("$.data.content[0].thumbnailUrl").isEmpty())
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.totalPages").value(1))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @Test
  void invalidPageSizeIsRejected() throws Exception {
    mockMvc.perform(get("/api/service-requests")
            .queryParam("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
  }
}
