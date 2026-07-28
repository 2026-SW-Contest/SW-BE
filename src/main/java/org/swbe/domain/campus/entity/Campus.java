package org.swbe.domain.campus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "campus")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Campus {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "campus_id") private Long id;
  @Column(name = "campus_name", nullable = false, unique = true, length = 100) private String name;
  @Column(length = 255) private String address;
  @Column(name = "is_active", nullable = false) private boolean active = true;
  @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
