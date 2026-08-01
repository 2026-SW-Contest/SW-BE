package org.swbe.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.swbe.global.error.ErrorCode;
import org.swbe.global.error.dto.ErrorResponse;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

  private final ObjectMapper objectMapper;

  public void write(
      HttpServletRequest request,
      HttpServletResponse response,
      ErrorCode errorCode
    ) throws IOException {
    response.setStatus(errorCode.status().value());
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    objectMapper.writeValue(
        response.getOutputStream(),
        ErrorResponse.of(errorCode, request.getRequestURI())
    );
  }
}
