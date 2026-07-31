package org.swbe.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "email_verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "verification_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private AppUser user;

  @Column(nullable = false, length = 255)
  private String email;

  @Column(name = "code_hash", nullable = false, length = 255)
  private String codeHash;

  @Column(nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private EmailVerificationPurpose purpose;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  @Column(name = "verification_token_hash", unique = true, length = 64)
  private String verificationTokenHash;

  @Column(name = "verification_token_expires_at")
  private LocalDateTime verificationTokenExpiresAt;

  @Column(name = "consumed_at")
  private LocalDateTime consumedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  private EmailVerification(
      String email,
      String codeHash,
      EmailVerificationPurpose purpose,
      LocalDateTime expiresAt,
      LocalDateTime createdAt
  ) {
    this.email = email;
    this.codeHash = codeHash;
    this.purpose = purpose;
    this.expiresAt = expiresAt;
    this.createdAt = createdAt;
  }

  public static EmailVerification createSignupVerification(
      String email,
      String codeHash,
      LocalDateTime expiresAt,
      LocalDateTime createdAt
  ) {
    return new EmailVerification(
        email,
        codeHash,
        EmailVerificationPurpose.SIGNUP,
        expiresAt,
        createdAt
    );
  }

  public void recordFailedAttempt() {
    attemptCount++;
  }

  public boolean hasReachedAttemptLimit(int maxAttempts) {
    return attemptCount >= maxAttempts;
  }

  public boolean isResendAllowed(LocalDateTime now, Duration cooldown) {
    return !createdAt.plus(cooldown).isAfter(now);
  }

  public void invalidate(LocalDateTime invalidatedAt) {
    expiresAt = invalidatedAt;
    verificationTokenHash = null;
    verificationTokenExpiresAt = null;
  }

  public void completeVerification(
      String tokenHash,
      LocalDateTime verifiedAt,
      LocalDateTime tokenExpiresAt
  ) {
    this.verifiedAt = verifiedAt;
    this.verificationTokenHash = tokenHash;
    this.verificationTokenExpiresAt = tokenExpiresAt;
  }

  public void consume(AppUser user, LocalDateTime consumedAt) {
    this.user = user;
    this.consumedAt = consumedAt;
  }

  public boolean isCodeExpired(LocalDateTime now) {
    return !expiresAt.isAfter(now);
  }

  public boolean isVerified() {
    return verifiedAt != null;
  }

  public boolean isTokenExpired(LocalDateTime now) {
    return verificationTokenExpiresAt == null
        || !verificationTokenExpiresAt.isAfter(now);
  }

  public boolean isConsumed() {
    return consumedAt != null;
  }
}
