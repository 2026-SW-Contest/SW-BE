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
    "Only the author can modify the facility request."
),
NOT_DELETABLE(
    HttpStatus.CONFLICT,
    "FACILITY_REQUEST_NOT_DELETABLE",
    "Only received facility requests can be deleted."
),
  NOT_EDITABLE(
      HttpStatus.CONFLICT,
      "FACILITY_REQUEST_NOT_EDITABLE",
      "Only received facility requests can be updated."
  ),
  INVALID_CURSOR(
      HttpStatus.BAD_REQUEST,
      "FACILITY_REQUEST_INVALID_CURSOR",
      "The facility request cursor is invalid."
  ),
  INVALID_REQUEST(
      HttpStatus.BAD_REQUEST,
      "INVALID_REQUEST",
      "At least one field or attachment must be updated."
  ),
  UPDATE_REQUIRED(
      HttpStatus.BAD_REQUEST,
      "FACILITY_REQUEST_UPDATE_REQUIRED",
      "A status or administrator response is required."
  ),
  INVALID_STATUS_TRANSITION(
      HttpStatus.CONFLICT,
      "FACILITY_REQUEST_INVALID_STATUS_TRANSITION",
      "The facility request status cannot be changed."
  ),
  ALREADY_COMPLETED(
      HttpStatus.CONFLICT,
      "FACILITY_REQUEST_ALREADY_COMPLETED",
      "The completed facility request cannot be changed."
  ),
  INVALID_ATTACHMENT(
      HttpStatus.BAD_REQUEST,
      "INVALID_ATTACHMENT",
      "An attachment does not belong to the facility request."
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
      "The attachment could not be processed."
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
