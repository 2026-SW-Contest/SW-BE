package org.swbe.global.error;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {

  VALIDATION_FAILED(
      HttpStatus.BAD_REQUEST,
      "COMMON_VALIDATION_FAILED",
      "요청 값 검증에 실패했습니다."
  ),
  MALFORMED_JSON(
      HttpStatus.BAD_REQUEST,
      "COMMON_MALFORMED_JSON",
      "요청 본문을 읽을 수 없습니다."
  ),
  TYPE_MISMATCH(
      HttpStatus.BAD_REQUEST,
      "COMMON_TYPE_MISMATCH",
      "요청 값의 형식이 올바르지 않습니다."
  ),
  MISSING_PARAMETER(
      HttpStatus.BAD_REQUEST,
      "COMMON_MISSING_PARAMETER",
      "필수 요청 파라미터가 누락되었습니다."
  ),
  RESOURCE_NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "COMMON_RESOURCE_NOT_FOUND",
      "요청한 리소스를 찾을 수 없습니다."
  ),
  METHOD_NOT_ALLOWED(
      HttpStatus.METHOD_NOT_ALLOWED,
      "COMMON_METHOD_NOT_ALLOWED",
      "지원하지 않는 HTTP 메서드입니다."
  ),
  UNSUPPORTED_MEDIA_TYPE(
      HttpStatus.UNSUPPORTED_MEDIA_TYPE,
      "COMMON_UNSUPPORTED_MEDIA_TYPE",
      "지원하지 않는 Content-Type입니다."
  ),
  INTERNAL_SERVER_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "COMMON_INTERNAL_SERVER_ERROR",
      "서버 내부 오류가 발생했습니다."
  );

  private final HttpStatus status;
  private final String code;
  private final String message;

  CommonErrorCode(HttpStatus status, String code, String message) {
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
