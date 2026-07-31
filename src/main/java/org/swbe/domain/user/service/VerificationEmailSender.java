package org.swbe.domain.user.service;

import java.time.Duration;

public interface VerificationEmailSender {

  void sendVerificationCode(String recipient, String code, Duration validity);
}
