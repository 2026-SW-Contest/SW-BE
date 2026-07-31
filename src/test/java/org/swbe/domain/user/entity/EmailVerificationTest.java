package org.swbe.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class EmailVerificationTest {

  private static final LocalDateTime CREATED_AT =
      LocalDateTime.of(2026, 7, 31, 12, 0);
  private static final LocalDateTime CODE_EXPIRES_AT =
      CREATED_AT.plusMinutes(5);

  @Test
  void createsSignupEmailVerification() {
    EmailVerification verification = createVerification();

    assertThat(verification.getEmail()).isEqualTo("student@mju.ac.kr");
    assertThat(verification.getCodeHash()).isEqualTo("{bcrypt}code-hash");
    assertThat(verification.getPurpose())
        .isEqualTo(EmailVerificationPurpose.SIGNUP);
    assertThat(verification.getAttemptCount()).isZero();
    assertThat(verification.getExpiresAt()).isEqualTo(CODE_EXPIRES_AT);
    assertThat(verification.getCreatedAt()).isEqualTo(CREATED_AT);
    assertThat(verification.isVerified()).isFalse();
    assertThat(verification.isConsumed()).isFalse();
  }

  @Test
  void determinesCodeExpirationAtBoundary() {
    EmailVerification verification = createVerification();

    assertThat(verification.isCodeExpired(CODE_EXPIRES_AT.minusNanos(1)))
        .isFalse();
    assertThat(verification.isCodeExpired(CODE_EXPIRES_AT)).isTrue();
  }

  @Test
  void recordsFailedAttempt() {
    EmailVerification verification = createVerification();

    verification.recordFailedAttempt();

    assertThat(verification.getAttemptCount()).isEqualTo(1);
  }

  @Test
  void determinesAttemptLimit() {
    EmailVerification verification = createVerification();

    for (int attempt = 0; attempt < 5; attempt++) {
      verification.recordFailedAttempt();
    }

    assertThat(verification.hasReachedAttemptLimit(5)).isTrue();
  }

  @Test
  void allowsResendAtCooldownBoundary() {
    EmailVerification verification = createVerification();
    Duration cooldown = Duration.ofMinutes(1);

    assertThat(verification.isResendAllowed(
        CREATED_AT.plus(cooldown).minusNanos(1),
        cooldown
    )).isFalse();
    assertThat(verification.isResendAllowed(
        CREATED_AT.plus(cooldown),
        cooldown
    )).isTrue();
  }

  @Test
  void invalidatesCodeAndIssuedToken() {
    EmailVerification verification = createVerification();
    LocalDateTime verifiedAt = CREATED_AT.plusSeconds(30);
    verification.completeVerification(
        "token-hash",
        verifiedAt,
        verifiedAt.plusMinutes(30)
    );
    LocalDateTime invalidatedAt = CREATED_AT.plusMinutes(1);

    verification.invalidate(invalidatedAt);

    assertThat(verification.getExpiresAt()).isEqualTo(invalidatedAt);
    assertThat(verification.getVerificationTokenHash()).isNull();
    assertThat(verification.getVerificationTokenExpiresAt()).isNull();
  }

  @Test
  void completesVerificationAndTracksTokenExpiration() {
    EmailVerification verification = createVerification();
    LocalDateTime verifiedAt = CREATED_AT.plusMinutes(1);
    LocalDateTime tokenExpiresAt = verifiedAt.plusMinutes(15);

    verification.completeVerification(
        "token-hash",
        verifiedAt,
        tokenExpiresAt
    );

    assertThat(verification.isVerified()).isTrue();
    assertThat(verification.getVerificationTokenHash())
        .isEqualTo("token-hash");
    assertThat(verification.getVerifiedAt()).isEqualTo(verifiedAt);
    assertThat(verification.isTokenExpired(tokenExpiresAt.minusNanos(1)))
        .isFalse();
    assertThat(verification.isTokenExpired(tokenExpiresAt)).isTrue();
  }

  @Test
  void consumesVerificationForCreatedUser() {
    EmailVerification verification = createVerification();
    AppUser user = mock(AppUser.class);
    LocalDateTime consumedAt = CREATED_AT.plusMinutes(2);

    verification.consume(user, consumedAt);

    assertThat(verification.getUser()).isSameAs(user);
    assertThat(verification.getConsumedAt()).isEqualTo(consumedAt);
    assertThat(verification.isConsumed()).isTrue();
  }

  private EmailVerification createVerification() {
    return EmailVerification.createSignupVerification(
        "student@mju.ac.kr",
        "{bcrypt}code-hash",
        CODE_EXPIRES_AT,
        CREATED_AT
    );
  }
}
