package org.swbe.global.error.dto;

import java.time.Instant;
import java.util.List;
import org.swbe.global.error.ErrorCode;

public record ErrorResponse(
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,
    List<FieldErrorResponse> fieldErrors
) {

  public ErrorResponse {
    fieldErrors = List.copyOf(fieldErrors);
  }

  public static ErrorResponse of(ErrorCode errorCode, String path) {
    return new ErrorResponse(
        Instant.now(),
        errorCode.status().value(),
        errorCode.code(),
        errorCode.message(),
        path,
        List.of()
    );
  }

  public static ErrorResponse validation(
      ErrorCode errorCode,
      String path,
      List<FieldErrorResponse> fieldErrors
  ) {
    return new ErrorResponse(
        Instant.now(),
        errorCode.status().value(),
        errorCode.code(),
        errorCode.message(),
        path,
        fieldErrors
    );
  }
}
