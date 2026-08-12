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
import org.swbe.domain.lostitem.dto.response.ItemClaimDecisionDataResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimDecisionResponse;
import org.swbe.domain.lostitem.service.ItemClaimDecisionService;
import org.swbe.domain.user.entity.AccountStatus;
import org.swbe.global.security.AppUserPrincipal;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(ItemClaimDecisionController.class)
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
class ItemClaimDecisionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ItemClaimDecisionService decisionService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void lostItemStaffCanApproveWithOptionalMessage() throws Exception {
    when(decisionService.decide(
        eq(31L),
        any(),
        eq(7L),
        eq(false)
    )).thenReturn(response());

    mockMvc.perform(patch("/api/item-claims/31/decision")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"decision\":\"APPROVED\"}")
            .with(user(principal("ROLE_LOST_ITEM_STAFF")))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.itemClaimId").value(31))
        .andExpect(jsonPath("$.data.decision").value("APPROVED"))
        .andExpect(jsonPath("$.data.decisionName").value("승인"));
  }

  @Test
  void adminFlagIsPassedToService() throws Exception {
    when(decisionService.decide(
        eq(31L),
        any(),
        eq(7L),
        eq(true)
    )).thenReturn(response());

    mockMvc.perform(patch("/api/item-claims/31/decision")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"decision\":\"APPROVED\"}")
            .with(user(principal("ROLE_ADMIN")))
            .with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void studentCannotDecideClaim() throws Exception {
    mockMvc.perform(patch("/api/item-claims/31/decision")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"decision\":\"APPROVED\"}")
            .with(user(principal("ROLE_STUDENT")))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void anonymousUserCannotDecideClaim() throws Exception {
    mockMvc.perform(patch("/api/item-claims/31/decision")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"decision\":\"APPROVED\"}")
            .with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void missingDecisionAndLongMessageAreRejected() throws Exception {
    AppUserPrincipal staff = principal("ROLE_LOST_ITEM_STAFF");

    mockMvc.perform(patch("/api/item-claims/31/decision")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}")
            .with(user(staff))
            .with(csrf()))
        .andExpect(status().isBadRequest());
    mockMvc.perform(patch("/api/item-claims/31/decision")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"decision":"REJECTED","message":"%s"}
                """.formatted("a".repeat(1001)))
            .with(user(staff))
            .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  private ItemClaimDecisionResponse response() {
    return new ItemClaimDecisionResponse(
        new ItemClaimDecisionDataResponse(
            31L,
            25L,
            "APPROVED",
            "승인",
            null,
            LocalDateTime.of(2026, 8, 12, 7, 30)
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
