package org.swbe.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AppUserTest {

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
}
