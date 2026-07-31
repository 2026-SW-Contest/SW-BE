package org.swbe.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.swbe.domain.user.dto.response.CsrfResponse;
import org.swbe.domain.user.dto.response.LoginResponse;
import org.swbe.domain.user.service.AuthService;
import org.swbe.global.error.GlobalExceptionHandler;

class AuthControllerTest {

  private AuthService authService;
  private AuthController authController;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    authService = mock(AuthService.class);
    authController = new AuthController(authService);
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
}
