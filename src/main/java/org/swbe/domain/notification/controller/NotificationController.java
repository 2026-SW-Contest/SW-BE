package org.swbe.domain.notification.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.notification.dto.response.NotificationListResponse;
import org.swbe.domain.notification.dto.response.NotificationUnreadCountResponse;
import org.swbe.domain.notification.service.NotificationService;
import org.swbe.global.security.AppUserPrincipal;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public NotificationListResponse getNotifications(
      @RequestParam(required = false) @Size(max = 512) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    return notificationService.getNotifications(
        principal.getUserId(),
        cursor,
        size
    );
  }

  @GetMapping("/unread-count")
  public NotificationUnreadCountResponse getUnreadCount(
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    return notificationService.getUnreadCount(principal.getUserId());
  }

  @PatchMapping("/read-all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void readAll(
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    notificationService.readAll(principal.getUserId());
  }

  @PatchMapping("/{notificationId}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void read(
      @PathVariable @Positive Long notificationId,
      @AuthenticationPrincipal AppUserPrincipal principal
  ) {
    notificationService.read(notificationId, principal.getUserId());
  }
}
