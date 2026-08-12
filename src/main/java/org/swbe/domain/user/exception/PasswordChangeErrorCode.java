package org.swbe.domain.user.exception;

import org.springframework.http.HttpStatus;
import org.swbe.global.error.ErrorCode;

public enum PasswordChangeErrorCode implements ErrorCode {

  CURRENT_PASSWORD_MISMATCH(
      HttpStatus.UNAUTHORIZED,
      "CURRENT_PASSWORD_MISMATCH",
      "현재 비밀번호가 일치하지 않습니다."
  ),
  PASSWORD_CONFIRMATION_MISMATCH(
      HttpStatus.BAD_REQUEST,
      "PASSWORD_CONFIRMATION_MISMATCH",
      "새 비밀번호와 비밀번호 확인값이 일치하지 않습니다."
  ),
  PASSWORD_REUSE_NOT_ALLOWED(
      HttpStatus.BAD_REQUEST,
      "PASSWORD_REUSE_NOT_ALLOWED",
      "현재 비밀번호와 다른 비밀번호를 입력해주세요."
  );

  private final HttpStatus status;
  private final String code;
  private final String message;

  PasswordChangeErrorCode(
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
