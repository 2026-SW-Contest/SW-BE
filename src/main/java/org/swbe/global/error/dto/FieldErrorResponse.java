package org.swbe.global.error.dto;

public record FieldErrorResponse(
    String field,
    String message
) {
}
