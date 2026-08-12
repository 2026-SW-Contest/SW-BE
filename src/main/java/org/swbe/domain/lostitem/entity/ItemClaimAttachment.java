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
@Table(name = "item_claim_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemClaimAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "attachment_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_claim_id")
  private ItemClaim itemClaim;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "file_id")
  private FileResource file;

  private ItemClaimAttachment(
      ItemClaim itemClaim,
      FileResource file
  ) {
    this.itemClaim = Objects.requireNonNull(itemClaim);
    this.file = Objects.requireNonNull(file);
  }

  public static ItemClaimAttachment attach(
      ItemClaim itemClaim,
      FileResource file
  ) {
    return new ItemClaimAttachment(itemClaim, file);
  }
}
