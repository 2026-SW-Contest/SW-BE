package org.swbe.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import org.swbe.global.error.ErrorCode;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private final SecurityErrorResponseWriter responseWriter;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException exception
  ) throws IOException {
    ErrorCode errorCode = exception instanceof CsrfException
        ? SecurityErrorCode.INVALID_CSRF_TOKEN
        : SecurityErrorCode.ACCESS_DENIED;

    responseWriter.write(request, response, errorCode);
  }
}
