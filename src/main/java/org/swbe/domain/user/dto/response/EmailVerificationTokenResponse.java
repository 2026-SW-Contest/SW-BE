package org.swbe.domain.user.dto.response;

import java.time.LocalDateTime;

public record EmailVerificationTokenResponse(
    String emailVerificationToken,
    LocalDateTime expiresAt
) {

}
