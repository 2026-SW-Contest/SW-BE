package org.swbe.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
  private String purpose;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
