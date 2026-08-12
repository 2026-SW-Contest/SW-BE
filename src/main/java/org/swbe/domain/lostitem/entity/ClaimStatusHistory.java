package org.swbe.domain.lostitem.entity;

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
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.swbe.domain.user.entity.AppUser;

@Entity
@Table(name = "claim_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClaimStatusHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "claim_status_history_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_claim_id")
  private ItemClaim itemClaim;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "changed_by")
  private AppUser changedBy;

  @Column(name = "actor_type", nullable = false, length = 20)
  private String actorType = "USER";

  @Column(name = "previous_status", length = 40)
  @Enumerated(EnumType.STRING)
  private ItemClaimStatus previousStatus;

  @Column(name = "new_status", nullable = false, length = 40)
  @Enumerated(EnumType.STRING)
  private ItemClaimStatus newStatus;

  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "change_reason")
  private String changeReason;

  @Column(name = "changed_at", nullable = false)
  private LocalDateTime changedAt;

  private ClaimStatusHistory(
      ItemClaim itemClaim,
      AppUser changedBy,
      ItemClaimStatus previousStatus,
      ItemClaimStatus newStatus,
      String changeReason,
      LocalDateTime changedAt
  ) {
    this.itemClaim = Objects.requireNonNull(itemClaim);
    this.changedBy = Objects.requireNonNull(changedBy);
    this.actorType = "USER";
    this.previousStatus = previousStatus;
    this.newStatus = Objects.requireNonNull(newStatus);
    this.changeReason = stripNullable(changeReason);
    this.changedAt = Objects.requireNonNull(changedAt);
  }

  public static ClaimStatusHistory recordInitial(
      ItemClaim itemClaim,
      AppUser changedBy,
      LocalDateTime changedAt
  ) {
    return new ClaimStatusHistory(
        itemClaim,
        changedBy,
        null,
        ItemClaimStatus.IN_PROGRESS,
        null,
        changedAt
    );
  }

  private static String stripNullable(String value) {
    if (value == null) {
      return null;
    }
    String stripped = value.strip();
    return stripped.isEmpty() ? null : stripped;
  }
}
