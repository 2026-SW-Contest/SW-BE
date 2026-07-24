package org.swbe.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.swbe.domain.campus.entity.Department;

@Entity @Table(name = "app_user")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id") private Long id;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id") private Department department;
  @Column(nullable = false, unique = true, length = 255) private String email;
  @Column(name = "password_hash", length = 255) private String passwordHash;
  @Column(nullable = false, length = 100) private String name;
  @Column(name = "student_number", unique = true, length = 30) private String studentNumber;
  @Column(name = "account_status", nullable = false, length = 30) private String accountStatus = "INVITED";
  @Column(name = "email_verified", nullable = false) private boolean emailVerified;
  @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
