package org.swbe.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.user.dto.response.CurrentUserDataResponse;
import org.swbe.domain.user.dto.response.CurrentUserDepartmentResponse;
import org.swbe.domain.user.dto.response.CurrentUserResponse;
import org.swbe.domain.user.entity.AccountStatus;
import org.swbe.domain.user.service.CurrentUserQueryService;
import org.swbe.domain.user.service.PasswordChangeService;
import org.swbe.global.security.AppUserPrincipal;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;
import org.swbe.global.security.UserSessionTerminator;

@WebMvcTest(UserController.class)
@Import({
    SecurityConfig.class,
    SecurityErrorResponseWriter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    RestSessionInformationExpiredStrategy.class,
    UserControllerTest.AdminTestController.class
})
@TestPropertySource(properties = {
    "app.security.frontend-origins[0]=http://localhost:3000"
})
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CurrentUserQueryService currentUserQueryService;

  @MockitoBean
  private PasswordChangeService passwordChangeService;

  @MockitoBean
  private UserSessionTerminator userSessionTerminator;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void authenticatedAdminCanGetCurrentUser() throws Exception {
    CurrentUserDepartmentResponse department =
        new CurrentUserDepartmentResponse(1L, "시설관리팀");
    CurrentUserDataResponse data = new CurrentUserDataResponse(
        10L,
        "커넥띵관리자",
        "admin@mju.ac.kr",
        null,
        department,
        List.of("ADMIN")
    );
    when(currentUserQueryService.getCurrentUser(10L))
        .thenReturn(new CurrentUserResponse(data));

    mockMvc.perform(get("/api/users/me")
            .session(authenticatedSession("ROLE_ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userId").value(10))
        .andExpect(jsonPath("$.data.name").value("커넥띵관리자"))
        .andExpect(jsonPath("$.data.department.departmentName")
            .value("시설관리팀"))
        .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"))
        .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
  }

  @Test
  void anonymousUserCannotGetCurrentUser() throws Exception {
    mockMvc.perform(get("/api/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code")
            .value("SECURITY_AUTHENTICATION_REQUIRED"));
  }

  @Test
  void authenticatedUserCanChangeOwnPassword() throws Exception {
    mockMvc.perform(patch("/api/users/me/password")
            .contentType("application/json")
            .content("""
                {
                  "currentPassword": "Current12!@",
                  "newPassword": "Changed34#$",
                  "newPasswordConfirm": "Changed34#$"
                }
                """)
            .session(authenticatedSession("ROLE_STUDENT"))
            .with(csrf()))
        .andExpect(status().isNoContent());

    verify(passwordChangeService).changePassword(eq(10L), any());
    verify(userSessionTerminator).terminateAll(
        any(),
        any(),
        any(),
        any()
    );
  }

  @Test
  void administratorCanChangeOwnPassword() throws Exception {
    mockMvc.perform(patch("/api/users/me/password")
            .contentType("application/json")
            .content("""
                {
                  "currentPassword": "Current12!@",
                  "newPassword": "Changed34#$",
                  "newPasswordConfirm": "Changed34#$"
                }
                """)
            .session(authenticatedSession("ROLE_ADMIN"))
            .with(csrf()))
        .andExpect(status().isNoContent());

    verify(passwordChangeService).changePassword(eq(10L), any());
  }

  @Test
  void anonymousUserCannotChangePassword() throws Exception {
    mockMvc.perform(patch("/api/users/me/password")
            .contentType("application/json")
            .content("""
                {
                  "currentPassword": "Current12!@",
                  "newPassword": "Changed34#$",
                  "newPasswordConfirm": "Changed34#$"
                }
                """)
            .with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code")
            .value("SECURITY_AUTHENTICATION_REQUIRED"));

    verifyNoInteractions(passwordChangeService);
  }

  @Test
  void weakNewPasswordIsRejected() throws Exception {
    mockMvc.perform(patch("/api/users/me/password")
            .contentType("application/json")
            .content("""
                {
                  "currentPassword": "Current12!@",
                  "newPassword": "password1",
                  "newPasswordConfirm": "password1"
                }
                """)
            .session(authenticatedSession("ROLE_STUDENT"))
            .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code")
            .value("COMMON_VALIDATION_FAILED"));

    verifyNoInteractions(passwordChangeService);
  }

  @Test
  void adminCanAccessAdminEndpoint() throws Exception {
    mockMvc.perform(get("/api/admin/test")
            .session(authenticatedSession("ROLE_ADMIN")))
        .andExpect(status().isNoContent());
  }

  @Test
  void studentCannotAccessAdminEndpoint() throws Exception {
    mockMvc.perform(get("/api/admin/test")
            .session(authenticatedSession("ROLE_STUDENT")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code")
            .value("SECURITY_ACCESS_DENIED"));
  }

  @Test
  void anonymousUserCannotAccessAdminEndpoint() throws Exception {
    mockMvc.perform(get("/api/admin/test"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code")
            .value("SECURITY_AUTHENTICATION_REQUIRED"));
  }

  private MockHttpSession authenticatedSession(String authority) {
    AppUserPrincipal principal = new AppUserPrincipal(
        10L,
        "admin@mju.ac.kr",
        "{bcrypt}password-hash",
        AccountStatus.ACTIVE,
        true,
        List.of(new SimpleGrantedAuthority(authority))
    );
    UsernamePasswordAuthenticationToken authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            principal.getAuthorities()
        );
    SecurityContext context =
        SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);

    MockHttpSession session = new MockHttpSession();
    session.setAttribute(
        HttpSessionSecurityContextRepository
            .SPRING_SECURITY_CONTEXT_KEY,
        context
    );
    return session;
  }

  @RestController
  @RequestMapping("/api/admin")
  static class AdminTestController {

    @GetMapping("/test")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void testAdminAccess() {
    }
  }
}
