package org.swbe.domain.lostitem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.swbe.domain.lostitem.dto.response.StoredItemStatusUpdateDataResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemStatusUpdateResponse;
import org.swbe.domain.lostitem.service.StoredItemStatusUpdateService;
import org.swbe.domain.user.entity.AccountStatus;
import org.swbe.global.security.AppUserPrincipal;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(StoredItemStatusController.class)
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
class StoredItemStatusControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private StoredItemStatusUpdateService statusUpdateService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void lostItemStaffCanChangeStatus() throws Exception {
    when(statusUpdateService.updateStatus(
        eq(25L),
        any(),
        eq(7L),
        eq(false)
    )).thenReturn(changedResponse());

    mockMvc.perform(patch("/api/stored-items/25/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                      "status": "IN_PROGRESS",
                      "changeReason": "소유자 확인 요청 접수"
                    }
                    """
            )
            .with(user(principal("ROLE_LOST_ITEM_STAFF")))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.storedItemId").value(25))
        .andExpect(jsonPath("$.data.previousStatus").value("STORED"))
        .andExpect(jsonPath("$.data.publicStatus")
            .value("IN_PROGRESS"))
        .andExpect(jsonPath("$.data.publicStatusName").value("진행중"))
        .andExpect(jsonPath("$.data.changed").value(true));
  }

  @Test
  void adminFlagIsPassedToService() throws Exception {
    when(statusUpdateService.updateStatus(
        eq(25L),
        any(),
        eq(7L),
        eq(true)
    )).thenReturn(changedResponse());

    mockMvc.perform(patch("/api/stored-items/25/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"IN_PROGRESS\"}")
            .with(user(principal("ROLE_ADMIN")))
            .with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void studentCannotChangeStatus() throws Exception {
    mockMvc.perform(patch("/api/stored-items/25/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"IN_PROGRESS\"}")
            .with(user(principal("ROLE_STUDENT")))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void anonymousUserCannotChangeStatus() throws Exception {
    mockMvc.perform(patch("/api/stored-items/25/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"IN_PROGRESS\"}")
            .with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void missingStatusIsRejected() throws Exception {
    mockMvc.perform(patch("/api/stored-items/25/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"changeReason\":\"사유\"}")
            .with(user(principal("ROLE_LOST_ITEM_STAFF")))
            .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code")
            .value("COMMON_VALIDATION_FAILED"));
  }

  @Test
  void unknownStatusIsRejectedAsTypeMismatch() throws Exception {
    mockMvc.perform(patch("/api/stored-items/25/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"UNKNOWN\"}")
            .with(user(principal("ROLE_LOST_ITEM_STAFF")))
            .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  private StoredItemStatusUpdateResponse changedResponse() {
    return new StoredItemStatusUpdateResponse(
        new StoredItemStatusUpdateDataResponse(
            25L,
            "STORED",
            "IN_PROGRESS",
            "진행중",
            true,
            LocalDateTime.of(2026, 8, 12, 16, 30)
        )
    );
  }

  private AppUserPrincipal principal(String authority) {
    return new AppUserPrincipal(
        7L,
        "staff@mju.ac.kr",
        "{noop}password",
        AccountStatus.ACTIVE,
        true,
        List.of(new SimpleGrantedAuthority(authority))
    );
  }
}
