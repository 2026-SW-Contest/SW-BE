package org.swbe.global.mail;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MailSenderProperties.class)
public class MailConfig {

}
