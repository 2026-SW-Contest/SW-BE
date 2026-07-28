package org.swbe.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "user_role")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRole {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_role_id") private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private AppUser user;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "role_id") private AppRole role;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "granted_by") private AppUser grantedBy;
  @Column(name = "granted_at", nullable = false) private LocalDateTime grantedAt;
  @Column(name = "revoked_at") private LocalDateTime revokedAt;
  @Column(name = "active_marker", insertable = false, updatable = false) private Integer activeMarker;
}
