package org.swbe.domain.facilityrequest.exception;

import org.springframework.http.HttpStatus;
import org.swbe.global.error.ErrorCode;

public enum FacilityRequestErrorCode implements ErrorCode {

  NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "FACILITY_REQUEST_NOT_FOUND",
      "The facility request was not found."
  ),
  CATEGORY_NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "FACILITY_CATEGORY_NOT_FOUND",
      "The active facility category was not found."
  ),
  LOCATION_NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "LOCATION_NOT_FOUND",
      "The active location was not found."
  ),
  ACCESS_DENIED(
      HttpStatus.FORBIDDEN,
      "FACILITY_REQUEST_ACCESS_DENIED",
      "Only the author can cancel the facility request."
  ),
  NOT_CANCELABLE(
      HttpStatus.CONFLICT,
      "FACILITY_REQUEST_NOT_CANCELABLE",
      "Only received facility requests can be canceled."
  ),
  FILE_LIMIT_EXCEEDED(
      HttpStatus.BAD_REQUEST,
      "FILE_LIMIT_EXCEEDED",
      "A maximum of five images can be attached."
  ),
  INVALID_FILE_TYPE(
      HttpStatus.BAD_REQUEST,
      "INVALID_FILE_TYPE",
      "Only image files can be attached."
  ),
  FILE_STORAGE_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "FILE_STORAGE_ERROR",
      "The attachment could not be stored."
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
