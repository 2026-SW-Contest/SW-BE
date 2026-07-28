package org.swbe.domain.lostitem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.swbe.domain.user.entity.AppUser;

@Entity
@Table(name = "claim_verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClaimVerification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "claim_verification_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_claim_id")
  private ItemClaim itemClaim;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "verified_by")
  private AppUser verifiedBy;

  @Column(name = "verification_type", nullable = false, length = 40)
  private String verificationType;

  @Column(name = "verification_result", nullable = false, length = 20)
  private String verificationResult;

  @Lob
  @Column(name = "verification_note")
  private String verificationNote;

  @Column(name = "verified_at", nullable = false)
  private LocalDateTime verifiedAt;
}
