package org.swbe.domain.facilityrequest.exception;

import org.springframework.http.HttpStatus;
import org.swbe.global.error.ErrorCode;

public enum FacilityRequestErrorCode implements ErrorCode {

  NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "FACILITY_REQUEST_NOT_FOUND",
      "시설·기자재 문의를 찾을 수 없습니다."
  );

  private final HttpStatus status;
  private final String code;
  private final String message;

  FacilityRequestErrorCode(
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
