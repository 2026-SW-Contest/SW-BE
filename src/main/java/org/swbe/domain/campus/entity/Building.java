package org.swbe.domain.campus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "building")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Building {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "building_id") private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "campus_id") private Campus campus;
  @Column(name = "building_name", nullable = false, length = 100) private String name;
  @Column(name = "building_code", unique = true, length = 30) private String code;
  @Column(name = "is_active", nullable = false) private boolean active = true;
  @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
