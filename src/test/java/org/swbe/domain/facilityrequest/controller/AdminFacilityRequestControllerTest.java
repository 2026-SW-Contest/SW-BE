package org.swbe.domain.facilityrequest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestListItemResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestListResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestLocationResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestPageResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestProcessDataResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestProcessResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestDetailDataResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestDetailResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestRequesterResponse;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestRequesterDetailResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityCategoryResponse;
import org.swbe.domain.facilityrequest.service.AdminFacilityRequestDetailService;
import org.swbe.domain.facilityrequest.service.AdminFacilityRequestProcessService;
import org.swbe.domain.facilityrequest.service.AdminFacilityRequestQueryService;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;
import org.swbe.global.security.AppUserPrincipal;
import org.swbe.domain.user.entity.AccountStatus;

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
  private AdminFacilityRequestDetailService detailService;

  @MockitoBean
  private AdminFacilityRequestProcessService processService;

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

  @Test
  void adminCanGetFacilityRequestDetail() throws Exception {
    AdminFacilityRequestDetailDataResponse data =
        new AdminFacilityRequestDetailDataResponse(
            25L,
            "Hallway light issue",
            "The hallway light keeps flickering.",
            new AdminFacilityRequestRequesterDetailResponse(
                7L,
                "Hong",
                "60241234",
                "student@mju.ac.kr"
            ),
            new FacilityCategoryResponse(1L, "Lighting"),
            new AdminFacilityRequestLocationResponse(
                2L,
                "S2",
                "Student Hall"
            ),
            "IN_PROGRESS",
            "In Progress",
            List.of(),
            List.of(),
            LocalDateTime.of(2026, 8, 12, 14, 30),
            LocalDateTime.of(2026, 8, 12, 15, 30)
        );
    when(detailService.getFacilityRequest(25L))
        .thenReturn(new AdminFacilityRequestDetailResponse(data));

    mockMvc.perform(get("/api/admin/facility-requests/25")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.facilityRequestId").value(25))
        .andExpect(jsonPath("$.data.requester.email")
            .value("student@mju.ac.kr"))
        .andExpect(jsonPath("$.data.attachments").isArray())
        .andExpect(jsonPath("$.data.adminResponses").isArray());
  }

  @Test
  void studentCannotGetAdminFacilityRequestDetail() throws Exception {
    mockMvc.perform(get("/api/admin/facility-requests/25")
            .with(user("student").roles("STUDENT")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code")
            .value("SECURITY_ACCESS_DENIED"));
  }

  @Test
  void invalidFacilityRequestIdIsRejected() throws Exception {
    mockMvc.perform(get("/api/admin/facility-requests/0")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code")
            .value("COMMON_VALIDATION_FAILED"));
  }

  @Test
  void adminCanProcessFacilityRequest() throws Exception {
    AdminFacilityRequestProcessDataResponse data =
        new AdminFacilityRequestProcessDataResponse(
            25L,
            "WAITING",
            "IN_PROGRESS",
            "진행 중",
            null,
            LocalDateTime.of(2026, 8, 12, 16, 30)
        );
    when(processService.process(eq(25L), any(), eq(7L)))
        .thenReturn(new AdminFacilityRequestProcessResponse(data));

    mockMvc.perform(patch("/api/admin/facility-requests/25")
            .contentType("application/json")
            .content("""
                {
                  "status": "IN_PROGRESS",
                  "adminResponse": "Inspection started."
                }
                """)
            .with(user(principal("ROLE_ADMIN")))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.previousStatus")
            .value("WAITING"))
        .andExpect(jsonPath("$.data.requestStatus")
            .value("IN_PROGRESS"));
  }

  @Test
  void studentCannotProcessFacilityRequest() throws Exception {
    mockMvc.perform(patch("/api/admin/facility-requests/25")
            .contentType("application/json")
            .content("{\"status\":\"IN_PROGRESS\"}")
            .with(user("student").roles("STUDENT"))
            .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code")
            .value("SECURITY_ACCESS_DENIED"));
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

  private AppUserPrincipal principal(String authority) {
    return new AppUserPrincipal(
        7L,
        "admin@mju.ac.kr",
        "{noop}password",
        AccountStatus.ACTIVE,
        true,
        List.of(new SimpleGrantedAuthority(authority))
    );
  }
}
