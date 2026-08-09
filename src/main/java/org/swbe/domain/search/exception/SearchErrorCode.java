package org.swbe.domain.search.exception;

import org.springframework.http.HttpStatus;
import org.swbe.global.error.ErrorCode;

public enum SearchErrorCode implements ErrorCode {

  INVALID_CURSOR(
      HttpStatus.BAD_REQUEST,
      "SEARCH_INVALID_CURSOR",
      "검색 커서의 형식이 올바르지 않습니다."
  );

  private final HttpStatus status;
  private final String code;
  private final String message;

  SearchErrorCode(
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
