package org.swbe.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.swbe.domain.user.dto.response.CsrfResponse;
import org.swbe.domain.user.dto.response.EmailVerificationTokenResponse;
import org.swbe.domain.user.dto.response.LoginResponse;
import org.swbe.domain.user.dto.response.SignupResponse;
import org.swbe.domain.user.service.AuthService;
import org.swbe.domain.user.service.EmailVerificationService;
import org.swbe.domain.user.service.SignupService;
import org.swbe.global.error.GlobalExceptionHandler;

class AuthControllerTest {

  private AuthService authService;
  private EmailVerificationService emailVerificationService;
  private SignupService signupService;
  private AuthController authController;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    authService = mock(AuthService.class);
    emailVerificationService = mock(EmailVerificationService.class);
    signupService = mock(SignupService.class);
    authController = new AuthController(
        authService,
        emailVerificationService,
        signupService
    );
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

  @Test
  void validSignupRequestCreatesStudentAccount() throws Exception {
    when(signupService.signup(any())).thenReturn(new SignupResponse(
        1L,
        "student@mju.ac.kr",
        "홍길동",
        "60241234",
        List.of("STUDENT")
    ));

    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validSignupJson("password1")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(1))
        .andExpect(jsonPath("$.email").value("student@mju.ac.kr"))
        .andExpect(jsonPath("$.studentNumber").value("60241234"))
        .andExpect(jsonPath("$.roles[0]").value("STUDENT"));
  }

  @Test
  void passwordConfirmationMismatchReturnsValidationError()
      throws Exception {
    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validSignupJson("different1")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field")
            .value("passwordConfirmed"));
  }

  private String validSignupJson(String passwordConfirm) {
    return """
        {
          "name": "홍길동",
          "studentNumber": "60241234",
          "email": "student@mju.ac.kr",
          "password": "password1",
          "passwordConfirm": "%s",
          "emailVerificationToken": "%s"
        }
        """.formatted(passwordConfirm, "a".repeat(43));
  }
}
