package org.swbe.domain.facilityrequest.controller;

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
import org.swbe.domain.facilityrequest.dto.response.FacilityCategoryResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestDetailDataResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestDetailResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListItemResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestLocationDetailResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestPageResponse;
import org.swbe.domain.facilityrequest.service.FacilityRequestDetailService;
import org.swbe.domain.facilityrequest.service.FacilityRequestQueryService;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(FacilityRequestController.class)
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
class FacilityRequestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private FacilityRequestQueryService facilityRequestQueryService;

  @MockitoBean
  private FacilityRequestDetailService facilityRequestDetailService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void anonymousUserCanGetFilteredFacilityRequestList() throws Exception {
    var item = new FacilityRequestListItemResponse(
        25L,
        "학생회관 1층 조명 깜빡임",
        "전기/조명",
        "학생회관",
        "IN_PROGRESS",
        "진행중",
        null,
        LocalDateTime.of(2026, 8, 1, 16, 0)
    );
    when(facilityRequestQueryService.getFacilityRequests(any())).thenReturn(
        new FacilityRequestListResponse(
            new FacilityRequestPageResponse(
                List.of(item),
                0,
                20,
                1,
                1,
                false
            )
        )
    );

    mockMvc.perform(get("/api/facility-requests")
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
        .andExpect(jsonPath("$.data.content[0].facilityRequestId").value(25))
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
    mockMvc.perform(get("/api/facility-requests")
            .queryParam("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
  }

  @Test
  void anonymousUserCanGetPublicFacilityRequestDetail() throws Exception {
    var data = new FacilityRequestDetailDataResponse(
        25L,
        "SR-20260801-0001",
        "Flickering hallway light",
        "The hallway light keeps flickering.",
        "LED light",
        new FacilityCategoryResponse(1L, "Electricity/Lighting"),
        new FacilityRequestLocationDetailResponse(2L, "Student Center"),
        "IN_PROGRESS",
        "In progress",
        List.of(),
        false,
        false,
        LocalDateTime.of(2026, 8, 1, 16, 0),
        LocalDateTime.of(2026, 8, 1, 16, 10)
    );
    when(facilityRequestDetailService.getFacilityRequest(25L, null, false))
        .thenReturn(new FacilityRequestDetailResponse(data));

    mockMvc.perform(get("/api/facility-requests/25"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.facilityRequestId").value(25))
        .andExpect(jsonPath("$.data.receiptNumber")
            .value("SR-20260801-0001"))
        .andExpect(jsonPath("$.data.category.categoryId").value(1))
        .andExpect(jsonPath("$.data.location.locationId").value(2))
        .andExpect(jsonPath("$.data.attachments.length()").value(0))
        .andExpect(jsonPath("$.data.editable").value(false))
        .andExpect(jsonPath("$.data.deletable").value(false));
  }
}
