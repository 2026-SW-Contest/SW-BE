package org.swbe.domain.facilityrequest.controller;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListItemResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestPageResponse;
import org.swbe.domain.facilityrequest.service.MyFacilityRequestQueryService;
import org.swbe.domain.user.entity.AccountStatus;
import org.swbe.global.security.AppUserPrincipal;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(MyFacilityRequestController.class)
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
class MyFacilityRequestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private MyFacilityRequestQueryService myFacilityRequestQueryService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void studentCanGetOwnFacilityRequests() throws Exception {
    FacilityRequestListItemResponse item =
        new FacilityRequestListItemResponse(
            25L,
            "Flickering hallway light",
            "Electricity/Lighting",
            "Student Center",
            "IN_PROGRESS",
            "In progress",
            "https://cdn.example.com/request-25.jpg",
            LocalDateTime.of(2026, 8, 12, 14, 30)
        );
    FacilityRequestListResponse response = new FacilityRequestListResponse(
        new FacilityRequestPageResponse(
            List.of(item),
            0,
            20,
            1,
            1,
            false
        )
    );
    when(myFacilityRequestQueryService.getMyFacilityRequests(7L, 0, 20))
        .thenReturn(response);

    mockMvc.perform(get("/api/users/me/facility-requests")
            .with(user(principal("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].facilityRequestId").value(25))
        .andExpect(jsonPath("$.data.content[0].requestStatus")
            .value("IN_PROGRESS"))
        .andExpect(jsonPath("$.data.content[0].thumbnailUrl")
            .value("https://cdn.example.com/request-25.jpg"))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @Test
  void anonymousUserCannotGetOwnFacilityRequests() throws Exception {
    mockMvc.perform(get("/api/users/me/facility-requests"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void nonStudentCannotGetOwnFacilityRequests() throws Exception {
    mockMvc.perform(get("/api/users/me/facility-requests")
            .with(user(principal("ROLE_ADMIN"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void invalidPageSizeIsRejected() throws Exception {
    mockMvc.perform(get("/api/users/me/facility-requests")
            .queryParam("size", "101")
            .with(user(principal("ROLE_STUDENT"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
  }

  private AppUserPrincipal principal(String authority) {
    return new AppUserPrincipal(
        7L,
        "student@mju.ac.kr",
        "{noop}password",
        AccountStatus.ACTIVE,
        true,
        List.of(new SimpleGrantedAuthority(authority))
    );
  }
}
