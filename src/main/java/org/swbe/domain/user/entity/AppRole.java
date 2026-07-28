package org.swbe.domain.user.entity;

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
@Table(name = "app_role")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppRole {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "role_id")
  private Long id;

  @Column(name = "role_code", nullable = false, unique = true, length = 50)
  private String code;

  @Column(name = "role_name", nullable = false, unique = true, length = 100)
  private String name;
}
