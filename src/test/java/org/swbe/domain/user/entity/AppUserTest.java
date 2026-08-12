package org.swbe.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AppUserTest {

  @Test
  void changesEncodedPasswordAndUpdatedAt() {
    LocalDateTime registeredAt = LocalDateTime.of(2026, 8, 1, 12, 0);
    LocalDateTime changedAt = LocalDateTime.of(2026, 8, 12, 18, 0);
    AppUser user = AppUser.registerStudent(
        "student@mju.ac.kr",
        "{bcrypt}old-password-hash",
        "홍길동",
        "60241234",
        registeredAt
    );

    user.changePasswordHash("{bcrypt}new-password-hash", changedAt);

    assertThat(user.getPasswordHash())
        .isEqualTo("{bcrypt}new-password-hash");
    assertThat(user.getUpdatedAt()).isEqualTo(changedAt);
  }

  private static final LocalDateTime REGISTERED_AT =
      LocalDateTime.of(2026, 7, 31, 12, 0);

  @Test
  void registersActiveEmailVerifiedStudent() {
    AppUser user = AppUser.registerStudent(
        "  STUDENT@MJU.AC.KR  ",
        "{bcrypt}encoded-password",
        "  홍길동  ",
        "  60241234  ",
        REGISTERED_AT
    );

    assertThat(user.getEmail()).isEqualTo("student@mju.ac.kr");
    assertThat(user.getPasswordHash())
        .isEqualTo("{bcrypt}encoded-password");
    assertThat(user.getName()).isEqualTo("홍길동");
    assertThat(user.getStudentNumber()).isEqualTo("60241234");
    assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(user.isEmailVerified()).isTrue();
    assertThat(user.getCreatedAt()).isEqualTo(REGISTERED_AT);
    assertThat(user.getUpdatedAt()).isEqualTo(REGISTERED_AT);
  }

  @Test
  void rejectsInvalidStudentNumber() {
    assertThatThrownBy(() -> AppUser.registerStudent(
        "student@mju.ac.kr",
        "{bcrypt}encoded-password",
        "홍길동",
        "1234",
        REGISTERED_AT
    )).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void withdrawAnonymizesAccountAndReleasesSignupIdentifiers() {
    AppUser withdrawnUser = registeredUser();
    ReflectionTestUtils.setField(withdrawnUser, "id", 10L);
    LocalDateTime withdrawnAt = REGISTERED_AT.plusDays(1);

    withdrawnUser.withdraw(withdrawnAt);

    assertThat(withdrawnUser.getEmail())
        .isEqualTo("withdrawn-10@users.invalid");
    assertThat(withdrawnUser.getPasswordHash()).isNull();
    assertThat(withdrawnUser.getName()).isEqualTo("탈퇴한 사용자");
    assertThat(withdrawnUser.getStudentNumber()).isNull();
    assertThat(withdrawnUser.getDepartment()).isNull();
    assertThat(withdrawnUser.getAccountStatus())
        .isEqualTo(AccountStatus.WITHDRAWN);
    assertThat(withdrawnUser.isEmailVerified()).isFalse();
    assertThat(withdrawnUser.getUpdatedAt()).isEqualTo(withdrawnAt);

    AppUser rejoinedUser = registeredUser();
    assertThat(rejoinedUser.getEmail()).isEqualTo("student@mju.ac.kr");
    assertThat(rejoinedUser.getStudentNumber()).isEqualTo("60241234");
  }

  @Test
  void transientUserCannotWithdraw() {
    assertThatThrownBy(() -> registeredUser().withdraw(REGISTERED_AT))
        .isInstanceOf(IllegalStateException.class);
  }

  private AppUser registeredUser() {
    return AppUser.registerStudent(
        "student@mju.ac.kr",
        "{bcrypt}encoded-password",
        "홍길동",
        "60241234",
        REGISTERED_AT
    );
  }
}
