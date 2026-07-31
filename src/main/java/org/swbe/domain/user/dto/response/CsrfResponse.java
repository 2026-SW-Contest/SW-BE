package org.swbe.domain.user.dto.response;

public record CsrfResponse(
    String headerName,
    String token
) {
}
