package org.swbe.domain.lostitem.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import org.swbe.domain.lostitem.dto.response.MyItemClaimListItemResponse;
import org.swbe.domain.lostitem.dto.response.MyItemClaimListResponse;
import org.swbe.domain.lostitem.dto.response.MyItemClaimSliceResponse;
import org.swbe.domain.lostitem.service.MyItemClaimQueryService;
import org.swbe.domain.user.entity.AccountStatus;
import org.swbe.global.security.AppUserPrincipal;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(MyItemClaimController.class)
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
class MyItemClaimControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private MyItemClaimQueryService myItemClaimQueryService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void studentCanGetOwnItemClaims() throws Exception {
    MyItemClaimListItemResponse item = new MyItemClaimListItemResponse(
        37L,
        15L,
        "Black wallet",
        "Wallet",
        "Student Center",
        LocalDate.of(2026, 8, 10),
        "ONLINE",
        "WAITING",
        "Waiting",
        "https://cdn.example.com/stored-item-15.jpg",
        null,
        LocalDateTime.of(2026, 8, 12, 14, 30),
        null
    );
    MyItemClaimListResponse response = new MyItemClaimListResponse(
        new MyItemClaimSliceResponse(List.of(item), "next-cursor", true)
    );
    when(myItemClaimQueryService.getMyItemClaims(7L, null, 20))
        .thenReturn(response);

    mockMvc.perform(get("/api/users/me/item-claims")
            .with(user(principal("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].itemClaimId").value(37))
        .andExpect(jsonPath("$.data.content[0].storedItemId").value(15))
        .andExpect(jsonPath("$.data.content[0].itemName")
            .value("Black wallet"))
        .andExpect(jsonPath("$.data.content[0].claimStatus")
            .value("WAITING"))
        .andExpect(jsonPath("$.data.content[0].thumbnailUrl")
            .value("https://cdn.example.com/stored-item-15.jpg"))
        .andExpect(jsonPath("$.data.nextCursor").value("next-cursor"))
        .andExpect(jsonPath("$.data.hasNext").value(true));
  }

  @Test
  void anonymousUserCannotGetOwnItemClaims() throws Exception {
    mockMvc.perform(get("/api/users/me/item-claims"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void nonStudentCannotGetOwnItemClaims() throws Exception {
    mockMvc.perform(get("/api/users/me/item-claims")
            .with(user(principal("ROLE_ADMIN"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void invalidSizeIsRejected() throws Exception {
    mockMvc.perform(get("/api/users/me/item-claims")
            .queryParam("size", "51")
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
