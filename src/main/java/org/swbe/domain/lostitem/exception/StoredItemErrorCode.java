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
