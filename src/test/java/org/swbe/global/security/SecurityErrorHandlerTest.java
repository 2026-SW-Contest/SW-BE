package org.swbe.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class SecurityErrorHandlerTest {

  private JsonMapper objectMapper;
  private RestAuthenticationEntryPoint authenticationEntryPoint;
  private RestAccessDeniedHandler accessDeniedHandler;

  @BeforeEach
  void setUp() {
    objectMapper = new JsonMapper();
    SecurityErrorResponseWriter responseWriter = new SecurityErrorResponseWriter(objectMapper);
    authenticationEntryPoint = new RestAuthenticationEntryPoint(responseWriter);
    accessDeniedHandler = new RestAccessDeniedHandler(responseWriter);
  }

  @Test
  void unauthenticatedRequestReturnsCommonErrorResponse() throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/protected");
    MockHttpServletResponse response = new MockHttpServletResponse();

    authenticationEntryPoint.commence(
        request,
        response,
        new AuthenticationCredentialsNotFoundException("not authenticated")
    );

    JsonNode body = objectMapper.readTree(response.getContentAsByteArray());

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
    assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
    assertThat(body.get("timestamp").asString()).isNotBlank();
    assertThat(body.get("status").asInt()).isEqualTo(401);
    assertThat(body.get("code").asString()).isEqualTo("SECURITY_AUTHENTICATION_REQUIRED");
    assertThat(body.get("message").asString()).isEqualTo("인증이 필요합니다.");
    assertThat(body.get("path").asString()).isEqualTo("/api/protected");
    assertThat(body.get("fieldErrors").isEmpty()).isTrue();
  }

  @Test
  void accessDeniedReturnsForbiddenErrorResponse() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin");
    MockHttpServletResponse response = new MockHttpServletResponse();

    accessDeniedHandler.handle(
        request,
        response,
        new AccessDeniedException("access denied")
    );

    JsonNode body = objectMapper.readTree(response.getContentAsByteArray());

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    assertThat(body.get("code").asString()).isEqualTo("SECURITY_ACCESS_DENIED");
    assertThat(body.get("path").asString()).isEqualTo("/api/admin");
  }

  @Test
  void missingCsrfTokenReturnsCsrfErrorResponse() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/protected");
    MockHttpServletResponse response = new MockHttpServletResponse();

    accessDeniedHandler.handle(
        request,
        response,
        new MissingCsrfTokenException(null)
    );

    JsonNode body = objectMapper.readTree(response.getContentAsByteArray());

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    assertThat(body.get("code").asString()).isEqualTo("SECURITY_INVALID_CSRF_TOKEN");
    assertThat(body.get("message").asString()).isEqualTo("CSRF 토큰이 없거나 올바르지 않습니다.");
    assertThat(body.get("path").asString()).isEqualTo("/api/protected");
  }
}
