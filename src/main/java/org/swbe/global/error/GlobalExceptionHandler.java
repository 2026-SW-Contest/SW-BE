package org.swbe.global.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.swbe.global.error.dto.ErrorResponse;
import org.swbe.global.error.dto.FieldErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log =
      LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(
      BusinessException exception,
      HttpServletRequest request
  ) {
    ErrorCode errorCode = exception.getErrorCode();

    return ResponseEntity.status(errorCode.status())
        .body(ErrorResponse.of(errorCode, request.getRequestURI()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpServletRequest request
  ) {
    List<FieldErrorResponse> fieldErrors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(this::toFieldErrorResponse)
            .distinct()
            .sorted((left, right) -> left.field().compareTo(right.field()))
            .toList();

    return validationResponse(request, fieldErrors);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ErrorResponse> handleMethodValidation(
      HandlerMethodValidationException exception,
      HttpServletRequest request
  ) {
    List<FieldErrorResponse> fieldErrors =
        exception.getParameterValidationResults().stream()
            .flatMap(result -> result.getResolvableErrors().stream()
                .map(error -> new FieldErrorResponse(
                    parameterName(result.getMethodParameter().getParameterName()),
                    defaultMessage(error)
                )))
            .toList();

    return validationResponse(request, fieldErrors);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException exception,
      HttpServletRequest request
  ) {
    List<FieldErrorResponse> fieldErrors =
        exception.getConstraintViolations().stream()
            .map(violation -> new FieldErrorResponse(
                violation.getPropertyPath().toString(),
                violation.getMessage()
            ))
            .toList();

    return validationResponse(request, fieldErrors);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableMessage(
      HttpMessageNotReadableException exception,
      HttpServletRequest request
  ) {
    return errorResponse(CommonErrorCode.MALFORMED_JSON, request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception,
      HttpServletRequest request
  ) {
    return errorResponse(CommonErrorCode.TYPE_MISMATCH, request);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParameter(
      MissingServletRequestParameterException exception,
      HttpServletRequest request
  ) {
    return errorResponse(CommonErrorCode.MISSING_PARAMETER, request);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResource(
      NoResourceFoundException exception,
      HttpServletRequest request
  ) {
    return errorResponse(CommonErrorCode.RESOURCE_NOT_FOUND, request);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
      HttpRequestMethodNotSupportedException exception,
      HttpServletRequest request
  ) {
    return errorResponse(CommonErrorCode.METHOD_NOT_ALLOWED, request);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
      HttpMediaTypeNotSupportedException exception,
      HttpServletRequest request
  ) {
    return errorResponse(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE, request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(
      Exception exception,
      HttpServletRequest request
  ) {
    log.error(
        "Unhandled exception: method={}, path={}",
        request.getMethod(),
        request.getRequestURI(),
        exception
    );

    return errorResponse(CommonErrorCode.INTERNAL_SERVER_ERROR, request);
  }

  private ResponseEntity<ErrorResponse> validationResponse(
      HttpServletRequest request,
      List<FieldErrorResponse> fieldErrors
  ) {
    CommonErrorCode errorCode = CommonErrorCode.VALIDATION_FAILED;

    return ResponseEntity.status(errorCode.status())
        .body(ErrorResponse.validation(
            errorCode,
            request.getRequestURI(),
            fieldErrors
        ));
  }

  private ResponseEntity<ErrorResponse> errorResponse(
      ErrorCode errorCode,
      HttpServletRequest request
  ) {
    return ResponseEntity.status(errorCode.status())
        .body(ErrorResponse.of(errorCode, request.getRequestURI()));
  }

  private FieldErrorResponse toFieldErrorResponse(FieldError fieldError) {
    return new FieldErrorResponse(
        fieldError.getField(),
        defaultMessage(fieldError)
    );
  }

  private String defaultMessage(MessageSourceResolvable error) {
    return Objects.requireNonNullElse(
        error.getDefaultMessage(),
        CommonErrorCode.VALIDATION_FAILED.message()
    );
  }

  private String parameterName(String name) {
    return Objects.requireNonNullElse(name, "parameter");
  }
}
