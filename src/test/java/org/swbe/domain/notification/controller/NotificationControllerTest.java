package org.swbe.domain.notification.controller;

import static org.mockito.Mockito.verify;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.swbe.domain.notification.dto.response.NotificationListItemResponse;
import org.swbe.domain.notification.dto.response.NotificationListResponse;
import org.swbe.domain.notification.dto.response.NotificationSliceResponse;
import org.swbe.domain.notification.service.NotificationService;
import org.swbe.domain.user.entity.AccountStatus;
import org.swbe.global.security.AppUserPrincipal;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(NotificationController.class)
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
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private NotificationService notificationService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void authenticatedUserGetsOwnNotificationList() throws Exception {
    when(notificationService.getNotifications(7L, null, 20))
        .thenReturn(listResponse());

    mockMvc.perform(get("/api/notifications")
            .with(user(principal())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].notificationId")
            .value(10))
        .andExpect(jsonPath("$.data.content[0].notificationType")
            .value("ITEM_CLAIM_DECIDED"))
        .andExpect(jsonPath("$.data.content[0].referenceType")
            .value("STORED_ITEM"))
        .andExpect(jsonPath("$.data.content[0].referenceId")
            .value(25))
        .andExpect(jsonPath("$.data.content[0].read").value(false))
        .andExpect(jsonPath("$.data.nextCursor").doesNotExist())
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @Test
  void authenticatedUserMarksOwnNotificationAsRead() throws Exception {
    mockMvc.perform(patch("/api/notifications/10/read")
            .with(user(principal()))
            .with(csrf()))
        .andExpect(status().isNoContent());

    verify(notificationService).read(10L, 7L);
  }

  @Test
  void anonymousUserCannotUseNotificationApi() throws Exception {
    mockMvc.perform(get("/api/notifications"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void readRequiresCsrfToken() throws Exception {
    mockMvc.perform(patch("/api/notifications/10/read")
            .with(user(principal())))
        .andExpect(status().isForbidden());
  }

  @Test
  void invalidSizeAndIdentifierAreRejected() throws Exception {
    AppUserPrincipal principal = principal();

    mockMvc.perform(get("/api/notifications")
            .param("size", "51")
            .with(user(principal)))
        .andExpect(status().isBadRequest());
    mockMvc.perform(patch("/api/notifications/0/read")
            .with(user(principal))
            .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  private NotificationListResponse listResponse() {
    return new NotificationListResponse(new NotificationSliceResponse(
        List.of(new NotificationListItemResponse(
            10L,
            "ITEM_CLAIM_DECIDED",
            "소유자 확인 요청이 승인되었습니다.",
            "학생증과 물품 특징을 확인했습니다.",
            "STORED_ITEM",
            25L,
            false,
            null,
            LocalDateTime.of(2026, 8, 12, 14, 30)
        )),
        null,
        false
    ));
  }

  private AppUserPrincipal principal() {
    return new AppUserPrincipal(
        7L,
        "student@mju.ac.kr",
        "{noop}password",
        AccountStatus.ACTIVE,
        true,
        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
    );
  }
}
