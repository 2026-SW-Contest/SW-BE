package org.swbe.domain.lostitem.exception;

import org.springframework.http.HttpStatus;
import org.swbe.global.error.ErrorCode;

public enum StoredItemErrorCode implements ErrorCode {

  NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "STORED_ITEM_NOT_FOUND",
      "The stored item was not found."
  ),
  INVALID_CURSOR(
      HttpStatus.BAD_REQUEST,
      "STORED_ITEM_INVALID_CURSOR",
      "The stored item cursor is invalid."
  ),
  OFFICE_NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "LOST_ITEM_OFFICE_NOT_FOUND",
      "The active lost item office was not found."
  ),
  CATEGORY_NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "ITEM_CATEGORY_NOT_FOUND",
      "The item category was not found."
  ),
  LOCATION_NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "LOCATION_NOT_FOUND",
      "The active location was not found."
  ),
  ACCESS_DENIED(
      HttpStatus.FORBIDDEN,
      "STORED_ITEM_ACCESS_DENIED",
      "The user is not assigned to the lost item office."
  ),
  INVALID_FOUND_LOCATION(
      HttpStatus.BAD_REQUEST,
      "STORED_ITEM_INVALID_FOUND_LOCATION",
      "Exactly one found location must be provided."
  ),
  FILE_LIMIT_EXCEEDED(
      HttpStatus.BAD_REQUEST,
      "STORED_ITEM_FILE_LIMIT_EXCEEDED",
      "A maximum of five images can be attached."
  ),
  INVALID_FILE_TYPE(
      HttpStatus.BAD_REQUEST,
      "STORED_ITEM_INVALID_FILE_TYPE",
      "Only JPEG, PNG, GIF, and WebP images can be attached."
  ),
  FILE_STORAGE_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "STORED_ITEM_FILE_STORAGE_ERROR",
      "The attachment could not be processed."
  ),
  INVALID_REQUEST(
      HttpStatus.BAD_REQUEST,
      "STORED_ITEM_INVALID_REQUEST",
      "At least one field or attachment must be updated."
  ),
  INVALID_ATTACHMENT(
      HttpStatus.BAD_REQUEST,
      "STORED_ITEM_INVALID_ATTACHMENT",
      "An attachment does not belong to the stored item."
  ),
  VERSION_CONFLICT(
      HttpStatus.CONFLICT,
      "STORED_ITEM_VERSION_CONFLICT",
      "The stored item was modified by another request."
  ),
  INVALID_STATUS_TRANSITION(
      HttpStatus.CONFLICT,
      "STORED_ITEM_INVALID_STATUS_TRANSITION",
      "The requested stored item status transition is not allowed."
  );

  private final HttpStatus status;
  private final String code;
  private final String message;

  StoredItemErrorCode(
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
