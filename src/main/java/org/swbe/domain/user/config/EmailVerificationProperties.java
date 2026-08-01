package org.swbe.domain.user.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.email-verification")
public record EmailVerificationProperties(
    Duration codeValidity,
    Duration resendCooldown,
    int maxAttempts,
    Duration tokenValidity
) {

}
