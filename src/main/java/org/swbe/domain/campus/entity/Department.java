package org.swbe.domain.campus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "department")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "department_id") private Long id;
  @Column(name = "department_name", nullable = false, unique = true, length = 100) private String name;
  @Column(name = "department_type", nullable = false, length = 30) private String type;
  @Column(name = "is_active", nullable = false) private boolean active = true;
  @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
