package org.swbe.domain.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.user.dto.response.CurrentUserResponse;
import org.swbe.domain.user.service.CurrentUserQueryService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final CurrentUserQueryService currentUserQueryService;

  // 현재 로그인한 사용자의 기본 정보와 활성 역할을 조회한다.
  @GetMapping("/me")
  public CurrentUserResponse getCurrentUser(
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    return currentUserQueryService.getCurrentUser(
        principal.getUserId()
    );
  }
}
