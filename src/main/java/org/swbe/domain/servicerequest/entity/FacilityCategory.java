package org.swbe.domain.servicerequest.entity;

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
@Table(name = "facility_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FacilityCategory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "facility_category_id")
  private Long id;

  @Column(name = "category_name", nullable = false, unique = true, length = 100)
  private String name;

  @Column(name = "category_type", nullable = false, length = 30)
  private String type;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;
}
