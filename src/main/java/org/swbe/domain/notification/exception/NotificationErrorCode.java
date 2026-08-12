package org.swbe.domain.notification.exception;

import org.springframework.http.HttpStatus;
import org.swbe.global.error.ErrorCode;

public enum NotificationErrorCode implements ErrorCode {

  NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "NOTIFICATION_NOT_FOUND",
      "The notification was not found."
  ),
  INVALID_CURSOR(
      HttpStatus.BAD_REQUEST,
      "NOTIFICATION_INVALID_CURSOR",
      "The notification cursor is invalid."
  );

  private final HttpStatus status;
  private final String code;
  private final String message;

  NotificationErrorCode(
      HttpStatus status,
      String code,
      String message
  ) {
    this.status = status;
    this.code = code;
    this.message = message;
  }

  @Override
  public HttpStatus status() {
    return status;
  }

  @Override
  public String code() {
    return code;
  }

  @Override
  public String message() {
    return message;
  }
}
