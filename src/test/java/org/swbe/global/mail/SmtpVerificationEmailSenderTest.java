package org.swbe.global.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpVerificationEmailSenderTest {

  @Test
  void sendsVerificationCodeWithConfiguredSenderAndSubject() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    MailSenderProperties properties = new MailSenderProperties(
        "no-reply@connecthing.example",
        "[Connecthing] 이메일 인증 코드 안내"
    );
    SmtpVerificationEmailSender emailSender =
        new SmtpVerificationEmailSender(mailSender, properties);

    emailSender.sendVerificationCode(
        "student@mju.ac.kr",
        "012345",
        Duration.ofMinutes(5)
    );

    ArgumentCaptor<SimpleMailMessage> messageCaptor =
        ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(messageCaptor.capture());
    SimpleMailMessage message = messageCaptor.getValue();

    assertThat(message.getFrom()).isEqualTo("no-reply@connecthing.example");
    assertThat(message.getTo()).containsExactly("student@mju.ac.kr");
    assertThat(message.getSubject())
        .isEqualTo("[Connecthing] 이메일 인증 코드 안내");
    assertThat(message.getText())
        .contains("안녕하세요, Connecthing입니다.")
        .contains("012345")
        .contains("발급 후 5분 동안 유효합니다.")
        .contains("본인이 요청하지 않은 경우");
  }
}
