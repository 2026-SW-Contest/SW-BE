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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.swbe.domain.campus.entity.Building;
import org.swbe.domain.campus.entity.Department;
import org.swbe.domain.campus.entity.Location;

@Entity
@Table(name = "lost_item_office")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LostItemOffice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "office_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "building_id")
  private Building building;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "department_id")
  private Department department;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "location_id")
  private Location location;

  @Column(name = "office_name", nullable = false, length = 100)
  private String name;

  @Column(name = "operating_hours", length = 255)
  private String operatingHours;

  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  private String guidance;

  @Column(name = "is_primary", nullable = false)
  private boolean primary;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @Column(name = "active_primary_marker", insertable = false, updatable = false)
  private Byte activePrimaryMarker;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
