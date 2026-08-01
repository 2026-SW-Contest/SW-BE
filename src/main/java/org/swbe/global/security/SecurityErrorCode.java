package org.swbe.global.security;

import org.springframework.http.HttpStatus;
import org.swbe.global.error.ErrorCode;

public enum SecurityErrorCode implements ErrorCode {

  AUTHENTICATION_REQUIRED(
      HttpStatus.UNAUTHORIZED,
      "SECURITY_AUTHENTICATION_REQUIRED",
      "인증이 필요합니다."
  ),
  ACCESS_DENIED(
      HttpStatus.FORBIDDEN,
      "SECURITY_ACCESS_DENIED",
      "요청을 수행할 권한이 없습니다."
  ),
  INVALID_CSRF_TOKEN(
      HttpStatus.FORBIDDEN,
      "SECURITY_INVALID_CSRF_TOKEN",
      "CSRF 토큰이 없거나 올바르지 않습니다."
  );

  private final HttpStatus status;
  private final String code;
  private final String message;

  SecurityErrorCode(HttpStatus status, String code, String message) {
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
