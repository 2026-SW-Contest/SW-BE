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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.user.entity.AppUser;

@Entity
@Table(name = "stored_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "stored_item_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "office_id")
  private LostItemOffice office;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "found_location_id")
  private Location foundLocation;

  @Column(name = "found_location_text", length = 255)
  private String foundLocationText;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "registered_by")
  private AppUser registeredBy;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_category_id")
  private ItemCategory itemCategory;

  @Column(name = "item_name", nullable = false, length = 150)
  private String itemName;

  @Enumerated(EnumType.STRING)
  @Column(name = "public_status", nullable = false, length = 30)
  private StoredItemStatus publicStatus = StoredItemStatus.STORED;

  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "public_description")
  private String publicDescription;

  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "private_description")
  private String privateDescription;

  @Column(name = "found_date", nullable = false)
  private LocalDate foundDate;

  @Column(name = "found_time")
  private LocalTime foundTime;

  @Column(name = "found_time_unknown", nullable = false)
  private boolean foundTimeUnknown;

  @Column(name = "received_at", nullable = false)
  private LocalDateTime receivedAt;

  @Column(name = "storage_position", length = 255)
  private String storagePosition;

  @Column(name = "storage_deadline")
  private LocalDate storageDeadline;

  @Column(name = "collected_at")
  private LocalDateTime collectedAt;

  @Column(name = "storage_closed_at")
  private LocalDateTime storageClosedAt;

  @Column(name = "storage_close_reason", length = 255)
  private String storageCloseReason;

  @Version
  private long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  private StoredItem(
      LostItemOffice office,
      Location foundLocation,
      String foundLocationText,
      AppUser registeredBy,
      ItemCategory itemCategory,
      String itemName,
      String publicDescription,
      String privateDescription,
      LocalDate foundDate,
      LocalDateTime registeredAt
  ) {
    this.office = Objects.requireNonNull(office);
    this.foundLocation = foundLocation;
    this.foundLocationText = stripNullable(foundLocationText);
    validateFoundLocation(foundLocation, this.foundLocationText);
    this.registeredBy = Objects.requireNonNull(registeredBy);
    this.itemCategory = Objects.requireNonNull(itemCategory);
    this.itemName = requireText(itemName, "itemName");
    this.publicStatus = StoredItemStatus.STORED;
    this.publicDescription = requireText(
        publicDescription,
        "publicDescription"
    );
    this.privateDescription = stripNullable(privateDescription);
    this.foundDate = Objects.requireNonNull(foundDate);
    this.foundTime = null;
    this.foundTimeUnknown = true;
    this.receivedAt = Objects.requireNonNull(registeredAt);
    this.storagePosition = null;
    this.storageDeadline = null;
    this.createdAt = registeredAt;
    this.updatedAt = registeredAt;
  }

  public static StoredItem create(
      LostItemOffice office,
      Location foundLocation,
      String foundLocationText,
      AppUser registeredBy,
      ItemCategory itemCategory,
      String itemName,
      String publicDescription,
      String privateDescription,
      LocalDate foundDate,
      LocalDateTime registeredAt
  ) {
    return new StoredItem(
        office,
        foundLocation,
        foundLocationText,
        registeredBy,
        itemCategory,
        itemName,
        publicDescription,
        privateDescription,
        foundDate,
        registeredAt
    );
  }

  public String getFoundLocationName() {
    return foundLocation == null
        ? foundLocationText
        : foundLocation.getName();
  }

  public void update(
      LostItemOffice office,
      ItemCategory itemCategory,
      Location foundLocation,
      String foundLocationText,
      boolean foundLocationChanged,
      String itemName,
      String publicDescription,
      String privateDescription,
      boolean privateDescriptionChanged,
      LocalDate foundDate,
      LocalDateTime updatedAt
  ) {
    if (office != null) {
      this.office = office;
    }
    if (itemCategory != null) {
      this.itemCategory = itemCategory;
    }
    if (foundLocationChanged) {
      String normalizedLocationText = stripNullable(foundLocationText);
      validateFoundLocation(foundLocation, normalizedLocationText);
      this.foundLocation = foundLocation;
      this.foundLocationText = normalizedLocationText;
    }
    if (itemName != null) {
      this.itemName = requireText(itemName, "itemName");
    }
    if (publicDescription != null) {
      this.publicDescription = requireText(
          publicDescription,
          "publicDescription"
      );
    }
    if (privateDescriptionChanged) {
      this.privateDescription = stripNullable(privateDescription);
    }
    if (foundDate != null) {
      this.foundDate = foundDate;
    }
    this.updatedAt = Objects.requireNonNull(updatedAt);
  }

  public boolean changeStatus(
      StoredItemStatus newStatus,
      LocalDateTime changedAt
  ) {
    Objects.requireNonNull(newStatus, "newStatus must not be null");
    if (publicStatus == newStatus) {
      return false;
    }
    if (!publicStatus.canTransitionTo(newStatus)) {
      throw new IllegalStateException(
          "Invalid stored item status transition: "
              + publicStatus + " -> " + newStatus
      );
    }
    this.publicStatus = newStatus;
    this.updatedAt = Objects.requireNonNull(changedAt);
    return true;
  }

  private static void validateFoundLocation(
      Location foundLocation,
      String foundLocationText
  ) {
    if ((foundLocation == null) == (foundLocationText == null)) {
      throw new IllegalArgumentException(
          "Exactly one found location must be provided"
      );
    }
  }

  private static String requireText(String value, String fieldName) {
    String stripped = stripNullable(value);
    if (stripped == null || stripped.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return stripped;
  }

  private static String stripNullable(String value) {
    if (value == null) {
      return null;
    }
    String stripped = value.strip();
    return stripped.isEmpty() ? null : stripped;
  }
}
