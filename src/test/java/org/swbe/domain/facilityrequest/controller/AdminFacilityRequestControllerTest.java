package org.swbe.domain.facilityrequest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestListItemResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestListResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestLocationResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestPageResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestRequesterResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityCategoryResponse;
import org.swbe.domain.facilityrequest.service.AdminFacilityRequestQueryService;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(AdminFacilityRequestController.class)
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
class AdminFacilityRequestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AdminFacilityRequestQueryService queryService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void adminCanGetFilteredFacilityRequestList() throws Exception {
    when(queryService.getFacilityRequests(any())).thenReturn(response());

    mockMvc.perform(get("/api/admin/facility-requests")
            .param("keyword", "light")
            .param("status", "IN_PROGRESS")
            .param("categoryId", "1")
            .param("locationId", "2")
            .param("from", "2026-08-01")
            .param("to", "2026-08-12")
            .param("page", "0")
            .param("size", "20")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].facilityRequestId")
            .value(25))
        .andExpect(jsonPath("$.data.content[0].requester.name")
            .value("Hong"))
        .andExpect(jsonPath("$.data.content[0].requester.studentNumber")
            .value("60241234"))
        .andExpect(jsonPath("$.data.content[0].category.categoryName")
            .value("Lighting"))
        .andExpect(jsonPath("$.data.content[0].location.locationCode")
            .value("S2"))
        .andExpect(jsonPath("$.data.content[0].requestStatus")
            .value("IN_PROGRESS"))
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }

  @Test
  void studentCannotGetAdminFacilityRequestList() throws Exception {
    mockMvc.perform(get("/api/admin/facility-requests")
            .with(user("student").roles("STUDENT")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code")
            .value("SECURITY_ACCESS_DENIED"));
  }

  @Test
  void anonymousUserCannotGetAdminFacilityRequestList() throws Exception {
    mockMvc.perform(get("/api/admin/facility-requests"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code")
            .value("SECURITY_AUTHENTICATION_REQUIRED"));
  }

  @Test
  void invalidPageSizeIsRejected() throws Exception {
    mockMvc.perform(get("/api/admin/facility-requests")
            .param("size", "101")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code")
            .value("COMMON_VALIDATION_FAILED"));
  }

  private AdminFacilityRequestListResponse response() {
    AdminFacilityRequestListItemResponse item =
        new AdminFacilityRequestListItemResponse(
            25L,
            "Hallway light issue",
            new AdminFacilityRequestRequesterResponse(
                7L,
                "Hong",
                "60241234"
            ),
            new FacilityCategoryResponse(1L, "Lighting"),
            new AdminFacilityRequestLocationResponse(
                2L,
                "S2",
                "Student Hall"
            ),
            "IN_PROGRESS",
            "In Progress",
            "https://cdn.example.com/request-25.jpg",
            LocalDateTime.of(2026, 8, 12, 14, 30)
        );
    AdminFacilityRequestPageResponse data =
        new AdminFacilityRequestPageResponse(
            List.of(item),
            0,
            20,
            1,
            1,
            false
        );
    return new AdminFacilityRequestListResponse(data);
  }
}
