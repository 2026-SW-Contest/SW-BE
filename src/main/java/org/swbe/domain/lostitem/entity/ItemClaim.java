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
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.swbe.domain.user.entity.AppUser;

@Entity
@Table(name = "item_claim")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemClaim {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "item_claim_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "stored_item_id")
  private StoredItem storedItem;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "claimant_user_id")
  private AppUser claimantUser;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "temporary_claimant_id")
  private TemporaryClaimant temporaryClaimant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by")
  private AppUser reviewedBy;

  @Column(name = "request_method", nullable = false, length = 30)
  private String requestMethod;

  @Enumerated(EnumType.STRING)
  @Column(name = "claim_status", nullable = false, length = 40)
  private ItemClaimStatus claimStatus = ItemClaimStatus.IN_PROGRESS;

  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "ownership_description")
  private String ownershipDescription;

  @Column(name = "expected_lost_location", length = 255)
  private String expectedLostLocation;

  @Column(name = "expected_lost_at")
  private LocalDateTime expectedLostAt;

  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "rejection_reason")
  private String rejectionReason;

  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "closure_reason")
  private String closureReason;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @Column(name = "collected_at")
  private LocalDateTime collectedAt;

  @Column(name = "canceled_at")
  private LocalDateTime canceledAt;

  @Version
  private long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  private ItemClaim(
      StoredItem storedItem,
      AppUser claimantUser,
      String ownershipDescription,
      LocalDateTime createdAt
  ) {
    this.storedItem = Objects.requireNonNull(storedItem);
    this.claimantUser = Objects.requireNonNull(claimantUser);
    this.temporaryClaimant = null;
    this.reviewedBy = null;
    this.requestMethod = "ONLINE";
    this.claimStatus = ItemClaimStatus.IN_PROGRESS;
    this.ownershipDescription = requireText(
        ownershipDescription,
        "ownershipDescription"
    );
    this.createdAt = Objects.requireNonNull(createdAt);
    this.updatedAt = createdAt;
  }

  public static ItemClaim createOnline(
      StoredItem storedItem,
      AppUser claimantUser,
      String ownershipDescription,
      LocalDateTime createdAt
  ) {
    return new ItemClaim(
        storedItem,
        claimantUser,
        ownershipDescription,
        createdAt
    );
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.strip();
  }
}
