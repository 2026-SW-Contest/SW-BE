package org.swbe.domain.lostitem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.swbe.domain.lostitem.dto.response.ItemClaimCreateDataResponse;
import org.swbe.domain.lostitem.dto.response.ItemClaimCreateResponse;
import org.swbe.domain.lostitem.service.ItemClaimCreateService;
import org.swbe.domain.user.entity.AccountStatus;
import org.swbe.global.security.AppUserPrincipal;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(ItemClaimCreateController.class)
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
class ItemClaimCreateControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ItemClaimCreateService itemClaimCreateService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void studentCreatesClaimWithImages() throws Exception {
    when(itemClaimCreateService.create(
        eq(25L),
        any(),
        any(),
        eq(7L)
    )).thenReturn(response());

    mockMvc.perform(multipart("/api/stored-items/25/claims")
            .file(requestPart(validRequestJson()))
            .file(image())
            .with(user(principal("ROLE_STUDENT")))
            .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.itemClaimId").value(31))
        .andExpect(jsonPath("$.data.storedItemId").value(25))
        .andExpect(jsonPath("$.data.claimantName").value("정석우"))
        .andExpect(jsonPath("$.data.studentNumber").value("60251423"))
        .andExpect(jsonPath("$.data.claimStatus")
            .value("IN_PROGRESS"))
        .andExpect(jsonPath("$.data.attachmentCount").value(1));
  }

  @Test
  void lostItemStaffCannotCreateOnlineClaim() throws Exception {
    mockMvc.perform(multipart("/api/stored-items/25/claims")
            .file(requestPart(validRequestJson()))
            .with(user(principal("ROLE_LOST_ITEM_STAFF")))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCannotCreateOnlineClaim() throws Exception {
    mockMvc.perform(multipart("/api/stored-items/25/claims")
            .file(requestPart(validRequestJson()))
            .with(user(principal("ROLE_ADMIN")))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void anonymousUserCannotCreateClaim() throws Exception {
    mockMvc.perform(multipart("/api/stored-items/25/claims")
            .file(requestPart(validRequestJson()))
            .with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void blankOwnershipDescriptionIsRejected() throws Exception {
    mockMvc.perform(multipart("/api/stored-items/25/claims")
            .file(requestPart("{\"ownershipDescription\":\" \"}"))
            .with(user(principal("ROLE_STUDENT")))
            .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code")
            .value("COMMON_VALIDATION_FAILED"));
  }

  @Test
  void nonPositiveStoredItemIdIsRejected() throws Exception {
    mockMvc.perform(multipart("/api/stored-items/0/claims")
            .file(requestPart(validRequestJson()))
            .with(user(principal("ROLE_STUDENT")))
            .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  private ItemClaimCreateResponse response() {
    return new ItemClaimCreateResponse(
        new ItemClaimCreateDataResponse(
            31L,
            25L,
            "정석우",
            "60251423",
            "IN_PROGRESS",
            1,
            LocalDateTime.of(2026, 8, 12, 5, 30)
        )
    );
  }

  private MockMultipartFile requestPart(String json) {
    return new MockMultipartFile(
        "request",
        "request.json",
        "application/json",
        json.getBytes(StandardCharsets.UTF_8)
    );
  }

  private MockMultipartFile image() {
    return new MockMultipartFile(
        "files",
        "proof.jpg",
        "image/jpeg",
        "image".getBytes(StandardCharsets.UTF_8)
    );
  }

  private String validRequestJson() {
    return """
        {
          "ownershipDescription": "지갑 내부 카드 정보를 확인해주세요."
        }
        """;
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
