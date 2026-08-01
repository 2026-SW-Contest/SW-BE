package org.swbe.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.user.entity.AccountStatus;

@WebMvcTest(
    controllers = SecurityLogoutIntegrationTest.ProtectedController.class
)
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
class SecurityLogoutIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private SessionRegistry sessionRegistry;

  @Autowired
  private SessionAuthenticationStrategy sessionAuthenticationStrategy;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void logoutInvalidatesSessionAndDeletesCookie() throws Exception {
    AppUserPrincipal principal = principal(1L);
    MockHttpSession session = authenticatedSession(principal);

    mockMvc.perform(post("/api/auth/logout")
            .session(session)
            .with(csrf()))
        .andExpect(status().isNoContent())
        .andExpect(cookie().maxAge("SESSION", 0));

    assertThat(session.isInvalid()).isTrue();
  }

  @Test
  void logoutWithoutCsrfTokenIsRejected() throws Exception {
    MockHttpSession session = authenticatedSession(principal(1L));

    mockMvc.perform(post("/api/auth/logout").session(session))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code")
            .value("SECURITY_INVALID_CSRF_TOKEN"));

    assertThat(session.isInvalid()).isFalse();
  }

  @Test
  void loginSessionStrategyRegistersAuthenticatedSession() {
    AppUserPrincipal principal = principal(1L);
    Authentication authentication = authentication(principal);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.getSession();

    sessionAuthenticationStrategy.onAuthentication(
        authentication,
        request,
        response
    );

    assertThat(sessionRegistry.getAllSessions(principal, false))
        .singleElement()
        .extracting(SessionInformation::getSessionId)
        .isEqualTo(request.getSession().getId());
  }

  @Test
  void expiredSessionReturnsUnauthorizedJsonResponse() throws Exception {
    AppUserPrincipal principal = principal(1L);
    MockHttpSession session = authenticatedSession(principal);
    sessionRegistry.registerNewSession(session.getId(), principal);
    SessionInformation sessionInformation =
        sessionRegistry.getSessionInformation(session.getId());
    sessionInformation.expireNow();

    mockMvc.perform(get("/api/protected").session(session))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code")
            .value("SECURITY_AUTHENTICATION_REQUIRED"));

    assertThat(session.isInvalid()).isTrue();
  }

  private MockHttpSession authenticatedSession(AppUserPrincipal principal) {
    MockHttpSession session = new MockHttpSession();
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication(principal));
    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
        context
    );
    return session;
  }

  private Authentication authentication(AppUserPrincipal principal) {
    return UsernamePasswordAuthenticationToken.authenticated(
        principal,
        null,
        principal.getAuthorities()
    );
  }

  private AppUserPrincipal principal(Long userId) {
    return new AppUserPrincipal(
        userId,
        "student@mju.ac.kr",
        "{bcrypt}password-hash",
        AccountStatus.ACTIVE,
        true,
        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
    );
  }

  @RestController
  static class ProtectedController {

    @GetMapping("/api/protected")
    Map<String, Boolean> protectedResource() {
      return Map.of("authenticated", true);
    }
  }
}
