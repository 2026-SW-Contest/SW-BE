package org.swbe.domain.user.exception;

import org.springframework.http.HttpStatus;
import org.swbe.global.error.ErrorCode;

public enum AuthErrorCode implements ErrorCode {

  INVALID_CREDENTIALS(
      HttpStatus.UNAUTHORIZED,
      "AUTH_INVALID_CREDENTIALS",
      "이메일 또는 비밀번호가 올바르지 않습니다."
  );

  private final HttpStatus status;
  private final String code;
  private final String message;

  AuthErrorCode(HttpStatus status, String code, String message) {
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
