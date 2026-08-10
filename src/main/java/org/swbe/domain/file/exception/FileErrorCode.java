package org.swbe.domain.file.exception;

import org.springframework.http.HttpStatus;
import org.swbe.global.error.ErrorCode;

public enum FileErrorCode implements ErrorCode {

  NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "FILE_NOT_FOUND",
      "The image file was not found."
  ),
  STORAGE_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "FILE_STORAGE_ERROR",
      "The image file could not be loaded."
  ),
  STORAGE_PROVIDER_NOT_SUPPORTED(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "FILE_STORAGE_PROVIDER_NOT_SUPPORTED",
      "The file storage provider is not supported."
  );

  private final HttpStatus status;
  private final String code;
  private final String message;

  FileErrorCode(
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
