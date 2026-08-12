package org.swbe.domain.lostitem.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.swbe.domain.lostitem.dto.request.StoredItemUpdateRequest;
import org.swbe.domain.lostitem.dto.response.StoredItemUpdateDataResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemUpdateResponse;
import org.swbe.domain.lostitem.service.StoredItemUpdateService;
import org.swbe.domain.user.entity.AccountStatus;
import org.swbe.global.security.AppUserPrincipal;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(StoredItemUpdateController.class)
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
class StoredItemUpdateControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private StoredItemUpdateService storedItemUpdateService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void lostItemStaffCanPatchItemWithImages() throws Exception {
    when(storedItemUpdateService.update(
        eq(25L),
        any(),
        any(),
        eq(7L),
        eq(false)
    )).thenReturn(response());

    mockMvc.perform(multipart("/api/stored-items/25")
            .file(requestPart(
                """
                    {
                      "itemName": "수정된 지갑",
                      "foundLocationText": "명진관 앞 벤치",
                      "privateDescription": "",
                      "keepFileIds": [32, 31]
                    }
                    """
            ))
            .file(image())
            .with(request -> {
              request.setMethod("PATCH");
              return request;
            })
            .with(user(principal("ROLE_LOST_ITEM_STAFF")))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.storedItemId").value(25))
        .andExpect(jsonPath("$.data.attachmentCount").value(3));

    ArgumentCaptor<StoredItemUpdateRequest> request =
        ArgumentCaptor.forClass(StoredItemUpdateRequest.class);
    verify(storedItemUpdateService).update(
        eq(25L),
        request.capture(),
        any(),
        eq(7L),
        eq(false)
    );
    assertThat(request.getValue().getItemName())
        .isEqualTo("수정된 지갑");
    assertThat(request.getValue().getKeepFileIds())
        .containsExactly(32L, 31L);
    assertThat(request.getValue().isPrivateDescriptionProvided()).isTrue();
    assertThat(request.getValue().getPrivateDescription()).isNull();
  }

  @Test
  void adminFlagIsPassedToService() throws Exception {
    when(storedItemUpdateService.update(
        eq(25L),
        any(),
        any(),
        eq(7L),
        eq(true)
    )).thenReturn(response());

    mockMvc.perform(multipart("/api/stored-items/25")
            .file(requestPart("{\"description\":\"수정 설명\"}"))
            .with(request -> {
              request.setMethod("PATCH");
              return request;
            })
            .with(user(principal("ROLE_ADMIN")))
            .with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void studentCannotPatchStoredItem() throws Exception {
    mockMvc.perform(multipart("/api/stored-items/25")
            .file(requestPart("{\"description\":\"수정 설명\"}"))
            .with(request -> {
              request.setMethod("PATCH");
              return request;
            })
            .with(user(principal("ROLE_STUDENT")))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void anonymousUserCannotPatchStoredItem() throws Exception {
    mockMvc.perform(multipart("/api/stored-items/25")
            .file(requestPart("{\"description\":\"수정 설명\"}"))
            .with(request -> {
              request.setMethod("PATCH");
              return request;
            })
            .with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void blankPublicDescriptionIsRejected() throws Exception {
    mockMvc.perform(multipart("/api/stored-items/25")
            .file(requestPart("{\"description\":\"   \"}"))
            .with(request -> {
              request.setMethod("PATCH");
              return request;
            })
            .with(user(principal("ROLE_LOST_ITEM_STAFF")))
            .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code")
            .value("COMMON_VALIDATION_FAILED"));
  }

  private StoredItemUpdateResponse response() {
    return new StoredItemUpdateResponse(
        new StoredItemUpdateDataResponse(
            25L,
            "STORED",
            3,
            LocalDateTime.of(2026, 8, 12, 15, 30)
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
        "new.jpg",
        "image/jpeg",
        "image".getBytes(StandardCharsets.UTF_8)
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
