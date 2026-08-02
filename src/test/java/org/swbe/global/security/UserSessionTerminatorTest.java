package org.swbe.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.swbe.domain.user.entity.AccountStatus;

class UserSessionTerminatorTest {

  @Test
  void terminatesEveryRegisteredSessionAndCurrentSession() {
    SessionRegistryImpl sessionRegistry = new SessionRegistryImpl();
    UserSessionTerminator terminator =
        new UserSessionTerminator(sessionRegistry);
    AppUserPrincipal principal = principal();
    Authentication authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            principal.getAuthorities()
        );
    MockHttpSession currentSession = new MockHttpSession();
    MockHttpSession otherSession = new MockHttpSession();
    sessionRegistry.registerNewSession(currentSession.getId(), principal);
    sessionRegistry.registerNewSession(otherSession.getId(), principal);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(currentSession);
    MockHttpServletResponse response = new MockHttpServletResponse();

    terminator.terminateAll(
        principal,
        authentication,
        request,
        response
    );

    assertThat(sessionRegistry.getAllSessions(principal, true))
        .extracting(SessionInformation::isExpired)
        .containsOnly(true);
    assertThat(currentSession.isInvalid()).isTrue();
    assertThat(response.getCookie("SESSION")).isNotNull();
    assertThat(response.getCookie("SESSION").getMaxAge()).isZero();
  }

  private AppUserPrincipal principal() {
    return new AppUserPrincipal(
        10L,
        "student@mju.ac.kr",
        "{bcrypt}password-hash",
        AccountStatus.ACTIVE,
        true,
        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
    );
  }
}
