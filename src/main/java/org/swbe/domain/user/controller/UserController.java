package org.swbe.domain.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.user.dto.request.PasswordChangeRequest;
import org.swbe.domain.user.dto.response.CurrentUserResponse;
import org.swbe.domain.user.service.CurrentUserQueryService;
import org.swbe.domain.user.service.PasswordChangeService;
import org.swbe.global.security.AppUserPrincipal;
import org.swbe.global.security.UserSessionTerminator;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final CurrentUserQueryService currentUserQueryService;
  private final PasswordChangeService passwordChangeService;
  private final UserSessionTerminator userSessionTerminator;

  // 현재 로그인한 사용자의 기본 정보와 활성 역할을 조회한다.
  @GetMapping("/me")
  public CurrentUserResponse getCurrentUser(
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    return currentUserQueryService.getCurrentUser(
        principal.getUserId()
    );
  }

  // 현재 사용자의 비밀번호를 변경하고 모든 로그인 세션을 종료한다.
  @PatchMapping("/me/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void changePassword(
      @Valid @RequestBody PasswordChangeRequest requestBody,
      @AuthenticationPrincipal AppUserPrincipal principal,
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    passwordChangeService.changePassword(
        principal.getUserId(),
        requestBody
    );
    userSessionTerminator.terminateAll(
        principal,
        authentication,
        request,
        response
    );
  }
}
