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
@Table(name = "staff_invitation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffInvitation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "invitation_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private AppUser user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private AppUser createdBy;

  @Column(name = "token_hash", nullable = false, unique = true, length = 255)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "used_at")
  private LocalDateTime usedAt;

  @Column(name = "revoked_at")
  private LocalDateTime revokedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
