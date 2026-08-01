package org.swbe.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSessionTerminator {

  private final SessionRegistry sessionRegistry;

  private final LogoutHandler currentSessionLogoutHandler =
      new CompositeLogoutHandler(
          new SecurityContextLogoutHandler(),
          new CookieClearingLogoutHandler("SESSION")
      );

  public void terminateAll(
      AppUserPrincipal principal,
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    sessionRegistry.getAllSessions(principal, false)
        .forEach(SessionInformation::expireNow);
    currentSessionLogoutHandler.logout(request, response, authentication);
  }
}
