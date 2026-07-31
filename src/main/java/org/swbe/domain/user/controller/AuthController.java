package org.swbe.domain.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.user.dto.request.LoginRequest;
import org.swbe.domain.user.dto.response.CsrfResponse;
import org.swbe.domain.user.dto.response.LoginResponse;
import org.swbe.domain.user.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @GetMapping("/csrf")
  public CsrfResponse csrf(CsrfToken csrfToken) {
    return new CsrfResponse(
        csrfToken.getHeaderName(),
        csrfToken.getToken()
    );
  }

  @PostMapping("/login")
  public LoginResponse login(
      @Valid @RequestBody LoginRequest loginRequest,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    return authService.login(loginRequest, request, response);
  }
}
