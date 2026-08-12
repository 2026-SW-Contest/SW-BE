package org.swbe.domain.user.exception;

import org.springframework.http.HttpStatus;
import org.swbe.global.error.ErrorCode;

public enum UserErrorCode implements ErrorCode {

  NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "USER_NOT_FOUND",
      "사용자를 찾을 수 없습니다."
  );

  private final HttpStatus status;
  private final String code;
  private final String message;

  UserErrorCode(
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
