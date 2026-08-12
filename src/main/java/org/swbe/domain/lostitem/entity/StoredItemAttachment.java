package org.swbe.domain.lostitem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.swbe.domain.file.entity.FileResource;

@Entity
@Table(name = "stored_item_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredItemAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "attachment_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "stored_item_id")
  private StoredItem storedItem;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "file_id")
  private FileResource file;

  @Column(name = "is_primary", nullable = false)
  private boolean primary;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  private StoredItemAttachment(
      StoredItem storedItem,
      FileResource file,
      boolean primary,
      int displayOrder
  ) {
    if (displayOrder < 0) {
      throw new IllegalArgumentException(
          "displayOrder must not be negative"
      );
    }
    this.storedItem = Objects.requireNonNull(storedItem);
    this.file = Objects.requireNonNull(file);
    this.primary = primary;
    this.displayOrder = displayOrder;
  }

  public static StoredItemAttachment attach(
      StoredItem storedItem,
      FileResource file,
      boolean primary,
      int displayOrder
  ) {
    return new StoredItemAttachment(
        storedItem,
        file,
        primary,
        displayOrder
    );
  }

  public void reorder(boolean primary, int displayOrder) {
    if (displayOrder < 0) {
      throw new IllegalArgumentException(
          "displayOrder must not be negative"
      );
    }
    this.primary = primary;
    this.displayOrder = displayOrder;
  }
}
