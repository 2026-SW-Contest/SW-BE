package org.swbe.global.mail;

import java.time.Duration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.swbe.domain.user.service.VerificationEmailSender;

@Component
public class SmtpVerificationEmailSender implements VerificationEmailSender {

  private static final String VERIFICATION_BODY = """
      안녕하세요, ConnecThing입니다.

      이메일 인증을 위한 인증 코드는 다음과 같습니다.

      %s

      인증 코드는 발급 후 %d분 동안 유효합니다.
      본인이 요청하지 않은 경우 이 메일을 무시해 주세요.
      """;

  private final JavaMailSender mailSender;
  private final MailSenderProperties properties;

  public SmtpVerificationEmailSender(
      JavaMailSender mailSender,
      MailSenderProperties properties
  ) {
    this.mailSender = mailSender;
    this.properties = properties;
  }

  @Override
  public void sendVerificationCode(
      String recipient,
      String code,
      Duration validity
  ) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(properties.from());
    message.setTo(recipient);
    message.setSubject(properties.verificationSubject());
    message.setText(VERIFICATION_BODY.formatted(code, validity.toMinutes()));

    mailSender.send(message);
  }
}
