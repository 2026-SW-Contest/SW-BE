package org.swbe.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.swbe.domain.user.dto.request.LoginRequest;
import org.swbe.domain.user.dto.response.LoginResponse;
import org.swbe.domain.user.entity.AccountStatus;
import org.swbe.domain.user.exception.AuthErrorCode;
import org.swbe.global.error.BusinessException;
import org.swbe.global.security.AppUserPrincipal;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private SessionAuthenticationStrategy sessionAuthenticationStrategy;

  @Mock
  private SecurityContextRepository securityContextRepository;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    authService = new AuthService(
        authenticationManager,
        sessionAuthenticationStrategy,
        securityContextRepository
    );
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void loginSavesAuthenticationInSessionAndReturnsUserSummary() {
    AppUserPrincipal principal = new AppUserPrincipal(
        1L,
        "student@example.com",
        "{bcrypt}encoded-password",
        AccountStatus.ACTIVE,
        true,
        List.of(
            new SimpleGrantedAuthority("ROLE_STUDENT"),
            new SimpleGrantedAuthority("ROLE_ADMIN"),
            new SimpleGrantedAuthority("ITEM_READ")
        )
    );
    Authentication authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            principal.getAuthorities()
        );
    when(authenticationManager.authenticate(any(Authentication.class)))
        .thenReturn(authentication);

    LoginResponse loginResponse = authService.login(
        new LoginRequest("student@example.com", "password"),
        request,
        response
    );

    assertThat(loginResponse.userId()).isEqualTo(1L);
    assertThat(loginResponse.email()).isEqualTo("student@example.com");
    assertThat(loginResponse.roles()).containsExactly("ADMIN", "STUDENT");

    verify(sessionAuthenticationStrategy).onAuthentication(
        authentication,
        request,
        response
    );

    ArgumentCaptor<SecurityContext> contextCaptor =
        ArgumentCaptor.forClass(SecurityContext.class);
    verify(securityContextRepository).saveContext(
        contextCaptor.capture(),
        eq(request),
        eq(response)
    );
    assertThat(contextCaptor.getValue().getAuthentication())
        .isSameAs(authentication);
    assertThat(SecurityContextHolder.getContext().getAuthentication())
        .isSameAs(authentication);
  }

  @Test
  void badCredentialsReturnsUnifiedAuthenticationError() {
    when(authenticationManager.authenticate(any(Authentication.class)))
        .thenThrow(new BadCredentialsException("bad credentials"));

    assertThatThrownBy(() -> authService.login(
        new LoginRequest("student@example.com", "wrong-password"),
        request,
        response
    ))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS)
        );

    verifyNoInteractions(
        sessionAuthenticationStrategy,
        securityContextRepository
    );
  }

  @Test
  void disabledAccountReturnsSameAuthenticationError() {
    when(authenticationManager.authenticate(any(Authentication.class)))
        .thenThrow(new DisabledException("disabled"));

    assertThatThrownBy(() -> authService.login(
        new LoginRequest("student@example.com", "password"),
        request,
        response
    ))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS)
        );
  }
}
