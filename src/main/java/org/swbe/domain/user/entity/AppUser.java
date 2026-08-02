package org.swbe.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.swbe.domain.campus.entity.Department;

@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser {

  private static final Pattern MJU_EMAIL_PATTERN = Pattern.compile(
      "(?i)^[A-Z0-9._%+-]+@mju\\.ac\\.kr$"
  );
  private static final Pattern STUDENT_NUMBER_PATTERN =
      Pattern.compile("\\d{8}");
  private static final int MIN_NAME_LENGTH = 2;
  private static final int MAX_NAME_LENGTH = 100;
  private static final String WITHDRAWN_EMAIL_FORMAT =
      "withdrawn-%d@users.invalid";
  private static final String WITHDRAWN_USER_NAME = "탈퇴한 사용자";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  private Department department;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", length = 255)
  private String passwordHash;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "student_number", unique = true, length = 30)
  private String studentNumber;

  @Column(name = "account_status", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private AccountStatus accountStatus = AccountStatus.INVITED;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  private AppUser(
      String email,
      String passwordHash,
      String name,
      String studentNumber,
      LocalDateTime registeredAt
  ) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.name = name;
    this.studentNumber = studentNumber;
    this.accountStatus = AccountStatus.ACTIVE;
    this.emailVerified = true;
    this.createdAt = registeredAt;
    this.updatedAt = registeredAt;
  }

  public static AppUser registerStudent(
      String email,
      String passwordHash,
      String name,
      String studentNumber,
      LocalDateTime registeredAt
  ) {
    String normalizedEmail = normalizeEmail(email);
    String normalizedName = stripNullable(name);
    String normalizedStudentNumber = stripNullable(studentNumber);
    validateStudentRegistration(
        normalizedEmail,
        passwordHash,
        normalizedName,
        normalizedStudentNumber,
        registeredAt
    );
    return new AppUser(
        normalizedEmail,
        passwordHash,
        normalizedName,
        normalizedStudentNumber,
        registeredAt
    );
  }

  public void withdraw(LocalDateTime withdrawnAt) {
    if (id == null) {
      throw new IllegalStateException("Only persisted users can withdraw");
    }
    if (accountStatus == AccountStatus.WITHDRAWN) {
      return;
    }

    email = WITHDRAWN_EMAIL_FORMAT.formatted(id);
    passwordHash = null;
    name = WITHDRAWN_USER_NAME;
    studentNumber = null;
    department = null;
    accountStatus = AccountStatus.WITHDRAWN;
    emailVerified = false;
    updatedAt = Objects.requireNonNull(
        withdrawnAt,
        "withdrawnAt must not be null"
    );
  }

  private static void validateStudentRegistration(
      String email,
      String passwordHash,
      String name,
      String studentNumber,
      LocalDateTime registeredAt
  ) {
    requireText(passwordHash, "passwordHash");
    Objects.requireNonNull(registeredAt, "registeredAt must not be null");

    if (email == null || !MJU_EMAIL_PATTERN.matcher(email).matches()) {
      throw new IllegalArgumentException("A valid MJU email is required");
    }
    if (studentNumber == null
        || !STUDENT_NUMBER_PATTERN.matcher(studentNumber).matches()) {
      throw new IllegalArgumentException(
          "Student number must consist of 8 digits"
      );
    }
    if (name == null
        || name.length() < MIN_NAME_LENGTH
        || name.length() > MAX_NAME_LENGTH
        || name.isBlank()) {
      throw new IllegalArgumentException(
          "Name length must be between 2 and 100"
      );
    }
  }

  private static void requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }

  private static String normalizeEmail(String email) {
    String stripped = stripNullable(email);
    return stripped == null ? null : stripped.toLowerCase(Locale.ROOT);
  }

  private static String stripNullable(String value) {
    return value == null ? null : value.strip();
  }
}
