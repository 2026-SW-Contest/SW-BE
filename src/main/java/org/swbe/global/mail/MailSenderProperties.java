package org.swbe.global.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailSenderProperties(
    String from,
    String verificationSubject
) {

}
