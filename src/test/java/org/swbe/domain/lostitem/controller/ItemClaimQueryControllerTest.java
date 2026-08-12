package org.swbe.domain.lostitem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.swbe.domain.lostitem.dto.response.ItemClaimDetailDataResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimDetailResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimListItemResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimListResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimSliceResponse;
import org.swbe.domain.lostitem.service.ItemClaimQueryService;
import org.swbe.domain.user.entity.AccountStatus;
import org.swbe.global.security.AppUserPrincipal;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(ItemClaimQueryController.class)
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
class ItemClaimQueryControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ItemClaimQueryService itemClaimQueryService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void lostItemStaffGetsFilteredClaimList() throws Exception {
    when(itemClaimQueryService.getItemClaims(
        eq(25L),
        any(),
        eq(7L),
        eq(false)
    )).thenReturn(listResponse());

    mockMvc.perform(get("/api/stored-items/25/claims")
            .param("status", "WAITING")
            .param("size", "20")
            .with(user(principal("ROLE_LOST_ITEM_STAFF"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].itemClaimId").value(31))
        .andExpect(jsonPath("$.data.content[0].claimantName")
            .value("정석우"))
        .andExpect(jsonPath("$.data.content[0].thumbnailUrl")
            .value("https://cdn/proof.jpg"))
        .andExpect(jsonPath("$.data.content[0].attachmentCount")
            .value(2));
  }

  @Test
  void adminFlagIsPassedForDetail() throws Exception {
    when(itemClaimQueryService.getItemClaim(31L, 7L, true))
        .thenReturn(detailResponse());

    mockMvc.perform(get("/api/item-claims/31")
            .with(user(principal("ROLE_ADMIN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.itemClaimId").value(31))
        .andExpect(jsonPath("$.data.ownershipDescription")
            .value("소유 증명 설명"));
  }

  @Test
  void studentCannotReadClaimListOrDetail() throws Exception {
    AppUserPrincipal student = principal("ROLE_STUDENT");

    mockMvc.perform(get("/api/stored-items/25/claims")
            .with(user(student)))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/item-claims/31")
            .with(user(student)))
        .andExpect(status().isForbidden());
  }

  @Test
  void anonymousUserCannotReadClaims() throws Exception {
    mockMvc.perform(get("/api/stored-items/25/claims"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void invalidStatusIsRejected() throws Exception {
    mockMvc.perform(get("/api/stored-items/25/claims")
            .param("status", "COLLECTED")
            .with(user(principal("ROLE_LOST_ITEM_STAFF"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void invalidSizeAndIdentifierAreRejected() throws Exception {
    AppUserPrincipal staff = principal("ROLE_LOST_ITEM_STAFF");

    mockMvc.perform(get("/api/stored-items/25/claims")
            .param("size", "51")
            .with(user(staff)))
        .andExpect(status().isBadRequest());
    mockMvc.perform(get("/api/item-claims/0")
            .with(user(staff)))
        .andExpect(status().isBadRequest());
  }

  private ItemClaimListResponse listResponse() {
    return new ItemClaimListResponse(new ItemClaimSliceResponse(
        List.of(new ItemClaimListItemResponse(
            31L,
            "정석우",
            "60251423",
            "ONLINE",
            "WAITING",
            "대기",
            "https://cdn/proof.jpg",
            2,
            LocalDateTime.of(2026, 8, 12, 15, 0)
        )),
        null,
        false
    ));
  }

  private ItemClaimDetailResponse detailResponse() {
    return new ItemClaimDetailResponse(new ItemClaimDetailDataResponse(
        31L,
        25L,
        "정석우",
        "60251423",
        "ONLINE",
        "소유 증명 설명",
        "WAITING",
        "대기",
        List.of(),
        List.of(),
        LocalDateTime.of(2026, 8, 12, 15, 0),
        LocalDateTime.of(2026, 8, 12, 15, 0)
    ));
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
