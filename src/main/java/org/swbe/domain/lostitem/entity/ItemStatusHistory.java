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
@Table(name = "item_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemStatusHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "item_status_history_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "stored_item_id")
  private StoredItem storedItem;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "changed_by")
  private AppUser changedBy;

  @Column(name = "actor_type", nullable = false, length = 20)
  private String actorType = "USER";

  @Enumerated(EnumType.STRING)
  @Column(name = "previous_status", length = 30)
  private StoredItemStatus previousStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", nullable = false, length = 30)
  private StoredItemStatus newStatus;

  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "change_reason")
  private String changeReason;

  @Column(name = "changed_at", nullable = false)
  private LocalDateTime changedAt;

  private ItemStatusHistory(
      StoredItem storedItem,
      AppUser changedBy,
      StoredItemStatus previousStatus,
      StoredItemStatus newStatus,
      String changeReason,
      LocalDateTime changedAt
  ) {
    this.storedItem = Objects.requireNonNull(storedItem);
    this.changedBy = Objects.requireNonNull(changedBy);
    this.actorType = "USER";
    this.previousStatus = previousStatus;
    this.newStatus = Objects.requireNonNull(newStatus);
    this.changeReason = stripNullable(changeReason);
    this.changedAt = Objects.requireNonNull(changedAt);
  }

  public static ItemStatusHistory recordInitial(
      StoredItem storedItem,
      AppUser changedBy,
      LocalDateTime changedAt
  ) {
    return new ItemStatusHistory(
        storedItem,
        changedBy,
        null,
        StoredItemStatus.STORED,
        null,
        changedAt
    );
  }

  public static ItemStatusHistory recordTransition(
      StoredItem storedItem,
      AppUser changedBy,
      StoredItemStatus previousStatus,
      StoredItemStatus newStatus,
      String changeReason,
      LocalDateTime changedAt
  ) {
    if (previousStatus == null || previousStatus == newStatus) {
      throw new IllegalArgumentException(
          "A status transition requires two different statuses"
      );
    }
    return new ItemStatusHistory(
        storedItem,
        changedBy,
        previousStatus,
        newStatus,
        changeReason,
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
