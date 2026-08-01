package org.swbe.domain.user.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.swbe.domain.user.dto.request.LoginRequest;
import org.swbe.domain.user.dto.response.LoginResponse;
import org.swbe.domain.user.exception.AuthErrorCode;
import org.swbe.global.error.BusinessException;
import org.swbe.global.security.AppUserPrincipal;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final String ROLE_PREFIX = "ROLE_";

  private final AuthenticationManager authenticationManager;
  private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
  private final SecurityContextRepository securityContextRepository;

  private final SecurityContextHolderStrategy securityContextHolderStrategy =
      SecurityContextHolder.getContextHolderStrategy();

  public LoginResponse login(
      LoginRequest loginRequest,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    Authentication authentication = authenticate(loginRequest);

    try {
      sessionAuthenticationStrategy.onAuthentication(
          authentication,
          request,
          response
      );

      SecurityContext securityContext =
          securityContextHolderStrategy.createEmptyContext();
      securityContext.setAuthentication(authentication);
      securityContextHolderStrategy.setContext(securityContext);

      securityContextRepository.saveContext(
          securityContext,
          request,
          response
      );

      return createLoginResponse(authentication);
    } catch (RuntimeException exception) {
      securityContextHolderStrategy.clearContext();
      throw exception;
    }
  }

  private Authentication authenticate(LoginRequest loginRequest) {
    try {
      return authenticationManager.authenticate(
          UsernamePasswordAuthenticationToken.unauthenticated(
              loginRequest.email(),
              loginRequest.password()
          )
      );
    } catch (BadCredentialsException | AccountStatusException exception) {
      throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
    }
  }

  private LoginResponse createLoginResponse(
      Authentication authentication
  ) {
    if (!(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
      throw new IllegalStateException("Unsupported authentication principal");
    }

    List<String> roles = principal.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(authority -> authority.startsWith(ROLE_PREFIX))
        .map(authority -> authority.substring(ROLE_PREFIX.length()))
        .sorted()
        .toList();

    return new LoginResponse(
        principal.getUserId(),
        principal.getUsername(),
        roles
    );
  }
}
