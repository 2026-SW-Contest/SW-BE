package org.swbe.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.swbe.domain.user.dto.response.CsrfResponse;
import org.swbe.domain.user.dto.response.EmailVerificationTokenResponse;
import org.swbe.domain.user.dto.response.LoginResponse;
import org.swbe.domain.user.service.AuthService;
import org.swbe.domain.user.service.EmailVerificationService;
import org.swbe.global.error.GlobalExceptionHandler;

class AuthControllerTest {

  private AuthService authService;
  private EmailVerificationService emailVerificationService;
  private AuthController authController;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    authService = mock(AuthService.class);
    emailVerificationService = mock(EmailVerificationService.class);
    authController = new AuthController(authService, emailVerificationService);
    mockMvc = MockMvcBuilders.standaloneSetup(authController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void csrfReturnsHeaderNameAndToken() {
    DefaultCsrfToken csrfToken = new DefaultCsrfToken(
        "X-CSRF-TOKEN",
        "_csrf",
        "csrf-token"
    );

    CsrfResponse csrfResponse = authController.csrf(csrfToken);

    org.assertj.core.api.Assertions.assertThat(csrfResponse.headerName())
        .isEqualTo("X-CSRF-TOKEN");
    org.assertj.core.api.Assertions.assertThat(csrfResponse.token())
        .isEqualTo("csrf-token");
  }

  @Test
  void validLoginRequestReturnsUserSummary() throws Exception {
    when(authService.login(any(), any(), any()))
        .thenReturn(new LoginResponse(
            1L,
            "student@example.com",
            List.of("STUDENT")
        ));

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "student@example.com",
                  "password": "password"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(1))
        .andExpect(jsonPath("$.email").value("student@example.com"))
        .andExpect(jsonPath("$.roles[0]").value("STUDENT"));
  }

  @Test
  void invalidLoginRequestReturnsValidationError() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "invalid-email",
                  "password": ""
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"))
        .andExpect(jsonPath("$.path").value("/api/auth/login"))
        .andExpect(jsonPath("$.fieldErrors.length()").value(2));
  }

  @Test
  void validMjuEmailSendsVerificationCode() throws Exception {
    mockMvc.perform(post("/api/auth/email-verifications")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "student@mju.ac.kr"
                }
                """))
        .andExpect(status().isNoContent());

    verify(emailVerificationService).sendVerificationCode(any());
  }

  @Test
  void nonMjuEmailCannotRequestVerificationCode() throws Exception {
    mockMvc.perform(post("/api/auth/email-verifications")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "student@example.com"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
  }

  @Test
  void validCodeReturnsSignupVerificationToken() throws Exception {
    LocalDateTime expiresAt = LocalDateTime.of(2026, 7, 31, 12, 30);
    when(emailVerificationService.confirmVerificationCode(any()))
        .thenReturn(new EmailVerificationTokenResponse(
            "signup-token",
            expiresAt
        ));

    mockMvc.perform(post("/api/auth/email-verifications/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "student@mju.ac.kr",
                  "code": "012345"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emailVerificationToken")
            .value("signup-token"))
        .andExpect(jsonPath("$.expiresAt")
            .value("2026-07-31T12:30:00"));
  }

  @Test
  void malformedVerificationCodeReturnsValidationError() throws Exception {
    mockMvc.perform(post("/api/auth/email-verifications/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "student@mju.ac.kr",
                  "code": "12345a"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("code"));
  }
}
