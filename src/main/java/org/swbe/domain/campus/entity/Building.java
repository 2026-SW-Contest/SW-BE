package org.swbe.domain.campus.entity;

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

@Entity
@Table(name = "building")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Building {

  private static final int LAST_DISPLAY_ORDER = Integer.MAX_VALUE;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "building_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "campus_id")
  private Campus campus;

  @Column(name = "building_name", nullable = false, length = 100)
  private String name;

  @Column(name = "building_code", unique = true, length = 30)
  private String code;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public int displayOrder() {
    if (code == null || !code.matches("S\\d+")) {
      return LAST_DISPLAY_ORDER;
    }

    return Integer.parseInt(code.substring(1));
  }
}
