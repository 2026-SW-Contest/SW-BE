package org.swbe.domain.campus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "location")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "location_id") private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "building_id") private Building building;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_location_id") private Location parent;
  @Column(name = "location_name", nullable = false, length = 100) private String name;
  @Column(length = 30) private String floor;
  @Column(length = 50) private String room;
  @Column(length = 255) private String description;
  @Column(name = "is_active", nullable = false) private boolean active = true;
  @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
