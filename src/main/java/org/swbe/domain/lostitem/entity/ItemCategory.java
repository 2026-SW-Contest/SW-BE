package org.swbe.domain.lostitem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "item_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemCategory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "item_category_id")
  private Long id;

  @Column(name = "category_name", nullable = false, unique = true, length = 100)
  private String name;

  @Column(name = "is_important_item", nullable = false)
  private boolean importantItem;

  @Column(name = "default_storage_days", nullable = false)
  private int defaultStorageDays = 90;

}
