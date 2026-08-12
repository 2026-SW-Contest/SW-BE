package org.swbe.domain.lostitem.exception;

import org.springframework.http.HttpStatus;
import org.swbe.global.error.ErrorCode;

public enum ItemClaimErrorCode implements ErrorCode {

  NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "ITEM_CLAIM_NOT_FOUND",
      "The ownership claim was not found."
  ),
  OFFICE_NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "LOST_ITEM_OFFICE_NOT_FOUND",
      "The lost item office was not found."
  ),
  ACCESS_DENIED(
      HttpStatus.FORBIDDEN,
      "ITEM_CLAIM_ACCESS_DENIED",
      "The user is not assigned to the lost item office."
  ),
  INVALID_CURSOR(
      HttpStatus.BAD_REQUEST,
      "ITEM_CLAIM_INVALID_CURSOR",
      "The ownership claim cursor is invalid."
  ),
  NOT_CLAIMABLE(
      HttpStatus.CONFLICT,
      "ITEM_CLAIM_NOT_CLAIMABLE",
      "A completed stored item cannot receive ownership claims."
  ),
  DUPLICATE_ACTIVE_CLAIM(
      HttpStatus.CONFLICT,
      "ITEM_CLAIM_DUPLICATE_ACTIVE_CLAIM",
      "The user already has an active claim for this stored item."
  ),
  INVALID_DECISION(
      HttpStatus.BAD_REQUEST,
      "ITEM_CLAIM_INVALID_DECISION",
      "Only APPROVED or REJECTED can be used as a decision."
  ),
  DECISION_MESSAGE_REQUIRED(
      HttpStatus.BAD_REQUEST,
      "ITEM_CLAIM_DECISION_MESSAGE_REQUIRED",
      "A rejection message is required."
  ),
  ALREADY_DECIDED(
      HttpStatus.CONFLICT,
      "ITEM_CLAIM_ALREADY_DECIDED",
      "The ownership claim has already been decided."
  ),
  VERSION_CONFLICT(
      HttpStatus.CONFLICT,
      "ITEM_CLAIM_VERSION_CONFLICT",
      "The ownership claim was modified by another request."
  ),
  FILE_LIMIT_EXCEEDED(
      HttpStatus.BAD_REQUEST,
      "ITEM_CLAIM_FILE_LIMIT_EXCEEDED",
      "A maximum of five images can be attached."
  ),
  INVALID_FILE_TYPE(
      HttpStatus.BAD_REQUEST,
      "ITEM_CLAIM_INVALID_FILE_TYPE",
      "Only JPEG, PNG, GIF, and WebP images can be attached."
  ),
  FILE_STORAGE_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "ITEM_CLAIM_FILE_STORAGE_ERROR",
      "The attachment could not be processed."
  );

  private final HttpStatus status;
  private final String code;
  private final String message;

  ItemClaimErrorCode(
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
