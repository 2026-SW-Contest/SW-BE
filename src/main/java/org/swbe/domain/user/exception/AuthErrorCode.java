package org.swbe.domain.user.exception;

import org.springframework.http.HttpStatus;
import org.swbe.global.error.ErrorCode;

public enum AuthErrorCode implements ErrorCode {

  INVALID_CREDENTIALS(
      HttpStatus.UNAUTHORIZED,
      "AUTH_INVALID_CREDENTIALS",
      "이메일 또는 비밀번호가 올바르지 않습니다."
  ),
  EMAIL_ALREADY_REGISTERED(
      HttpStatus.CONFLICT,
      "AUTH_EMAIL_ALREADY_REGISTERED",
      "이미 가입된 이메일입니다."
  ),
  EMAIL_VERIFICATION_RESEND_TOO_SOON(
      HttpStatus.TOO_MANY_REQUESTS,
      "AUTH_EMAIL_VERIFICATION_RESEND_TOO_SOON",
      "인증 코드 재발송은 1분 후에 가능합니다."
  ),
  EMAIL_VERIFICATION_NOT_FOUND(
      HttpStatus.BAD_REQUEST,
      "AUTH_EMAIL_VERIFICATION_NOT_FOUND",
      "이메일 인증 요청을 찾을 수 없습니다."
  ),
  EMAIL_VERIFICATION_EXPIRED(
      HttpStatus.BAD_REQUEST,
      "AUTH_EMAIL_VERIFICATION_EXPIRED",
      "인증 코드가 만료되었습니다."
  ),
  EMAIL_VERIFICATION_CODE_MISMATCH(
      HttpStatus.BAD_REQUEST,
      "AUTH_EMAIL_VERIFICATION_CODE_MISMATCH",
      "인증 코드가 올바르지 않습니다."
  ),
  EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED(
      HttpStatus.TOO_MANY_REQUESTS,
      "AUTH_EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED",
      "인증 코드 입력 가능 횟수를 초과했습니다."
  ),
  EMAIL_VERIFICATION_ALREADY_COMPLETED(
      HttpStatus.CONFLICT,
      "AUTH_EMAIL_VERIFICATION_ALREADY_COMPLETED",
      "이미 완료된 이메일 인증입니다."
  ),
  EMAIL_SEND_FAILED(
      HttpStatus.BAD_GATEWAY,
      "AUTH_EMAIL_SEND_FAILED",
      "인증 메일을 발송하지 못했습니다."
  ),
  STUDENT_NUMBER_ALREADY_REGISTERED(
      HttpStatus.CONFLICT,
      "AUTH_STUDENT_NUMBER_ALREADY_REGISTERED",
      "이미 가입에 사용된 학번입니다."
  ),
  INVALID_EMAIL_VERIFICATION_TOKEN(
      HttpStatus.BAD_REQUEST,
      "AUTH_INVALID_EMAIL_VERIFICATION_TOKEN",
      "이메일 인증 토큰이 올바르지 않습니다."
  ),
  EMAIL_VERIFICATION_TOKEN_EXPIRED(
      HttpStatus.BAD_REQUEST,
      "AUTH_EMAIL_VERIFICATION_TOKEN_EXPIRED",
      "이메일 인증 토큰이 만료되었습니다."
  ),
  EMAIL_VERIFICATION_TOKEN_CONSUMED(
      HttpStatus.CONFLICT,
      "AUTH_EMAIL_VERIFICATION_TOKEN_CONSUMED",
      "이미 사용된 이메일 인증 토큰입니다."
  ),
  EMAIL_VERIFICATION_EMAIL_MISMATCH(
      HttpStatus.BAD_REQUEST,
      "AUTH_EMAIL_VERIFICATION_EMAIL_MISMATCH",
      "인증한 이메일과 가입 이메일이 일치하지 않습니다."
  ),
  SIGNUP_CONFLICT(
      HttpStatus.CONFLICT,
      "AUTH_SIGNUP_CONFLICT",
      "이메일 또는 학번이 이미 가입에 사용되었습니다."
  ),
  ACCOUNT_NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "AUTH_ACCOUNT_NOT_FOUND",
      "사용자 계정을 찾을 수 없습니다."
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
