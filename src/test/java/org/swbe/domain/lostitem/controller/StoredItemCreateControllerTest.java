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
import org.swbe.domain.lostitem.dto.response.StoredItemCreateDataResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemCreateResponse;
import org.swbe.domain.lostitem.service.StoredItemCreateService;
import org.swbe.domain.user.entity.AccountStatus;
import org.swbe.global.security.AppUserPrincipal;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(StoredItemCreateController.class)
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
class StoredItemCreateControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private StoredItemCreateService storedItemCreateService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void lostItemStaffCanCreateItemWithImages() throws Exception {
    when(storedItemCreateService.create(
        any(),
        any(),
        eq(7L),
        eq(false)
    )).thenReturn(response());

    mockMvc.perform(multipart("/api/lost-item")
            .file(requestPart(validRequestJson()))
            .file(image())
            .with(user(principal("ROLE_LOST_ITEM_STAFF")))
            .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.storedItemId").value(25))
        .andExpect(jsonPath("$.data.publicStatus").value("STORED"))
        .andExpect(jsonPath("$.data.attachmentCount").value(1));
  }

  @Test
  void adminFlagIsPassedToService() throws Exception {
    when(storedItemCreateService.create(
        any(),
        any(),
        eq(7L),
        eq(true)
    )).thenReturn(response());

    mockMvc.perform(multipart("/api/lost-item")
            .file(requestPart(validRequestJson()))
            .with(user(principal("ROLE_ADMIN")))
            .with(csrf()))
        .andExpect(status().isCreated());
  }

  @Test
  void studentCannotCreateStoredItem() throws Exception {
    mockMvc.perform(multipart("/api/lost-item")
            .file(requestPart(validRequestJson()))
            .with(user(principal("ROLE_STUDENT")))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void anonymousUserCannotCreateStoredItem() throws Exception {
    mockMvc.perform(multipart("/api/lost-item")
            .file(requestPart(validRequestJson()))
            .with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void invalidRequestPartIsRejected() throws Exception {
    mockMvc.perform(multipart("/api/lost-item")
            .file(requestPart(
                """
                    {
                      "officeId": 3,
                      "categoryId": 2,
                      "foundLocationText": "명진관 앞 벤치",
                      "itemName": " ",
                      "description": "공개 설명",
                      "foundDate": "2026-08-12"
                    }
                    """
            ))
            .with(user(principal("ROLE_LOST_ITEM_STAFF")))
            .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code")
            .value("COMMON_VALIDATION_FAILED"));
  }

  @Test
  void legacyPluralPostPathIsNotExposed() throws Exception {
    mockMvc.perform(multipart("/api/stored-items")
            .file(requestPart(validRequestJson()))
            .with(user(principal("ROLE_LOST_ITEM_STAFF")))
            .with(csrf()))
        .andExpect(status().isNotFound());
  }

  private StoredItemCreateResponse response() {
    return new StoredItemCreateResponse(
        new StoredItemCreateDataResponse(
            25L,
            "STORED",
            1,
            LocalDateTime.of(2026, 8, 12, 14, 30)
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
        "wallet.jpg",
        "image/jpeg",
        "image".getBytes(StandardCharsets.UTF_8)
    );
  }

  private String validRequestJson() {
    return """
        {
          "officeId": 3,
          "categoryId": 2,
          "foundLocationId": 10,
          "foundLocationText": "1층 엘리베이터 앞",
          "itemName": "검은색 지갑",
          "description": "학생증과 카드가 들어 있습니다.",
          "privateDescription": "내부 확인용 메모",
          "foundDate": "2026-08-12"
        }
        """;
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
