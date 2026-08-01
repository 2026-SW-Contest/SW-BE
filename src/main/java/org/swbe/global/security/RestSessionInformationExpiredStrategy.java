package org.swbe.global.security;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestSessionInformationExpiredStrategy
    implements SessionInformationExpiredStrategy {

  private final SecurityErrorResponseWriter responseWriter;

  @Override
  public void onExpiredSessionDetected(
      SessionInformationExpiredEvent event
  ) throws IOException {
    responseWriter.write(
        event.getRequest(),
        event.getResponse(),
        SecurityErrorCode.AUTHENTICATION_REQUIRED
    );
  }
}
