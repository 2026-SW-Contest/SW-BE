package org.swbe.domain.user.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record SignupRequest(
    @NotBlank(message = "이름은 필수입니다.")
    @Size(
        min = 2,
        max = 100,
        message = "이름은 2자 이상 100자 이하여야 합니다."
    )
    String name,

    @NotBlank(message = "학번은 필수입니다.")
    @Pattern(
        regexp = "\\d{8}",
        message = "학번은 8자리 숫자여야 합니다."
    )
    String studentNumber,

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 255, message = "이메일은 255자를 초과할 수 없습니다.")
    @Pattern(
        regexp = "(?i)^[A-Z0-9._%+-]+@mju\\.ac\\.kr$",
        message = "mju.ac.kr 이메일만 사용할 수 있습니다."
    )
    String email,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(
        min = 8,
        max = 64,
        message = "비밀번호는 8자 이상 64자 이하여야 합니다."
    )
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "비밀번호는 영문과 숫자를 각각 하나 이상 포함해야 합니다."
    )
    String password,

    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    @Size(
        max = 64,
        message = "비밀번호 확인은 64자를 초과할 수 없습니다."
    )
    String passwordConfirm,

    @NotBlank(message = "이메일 인증 토큰은 필수입니다.")
    @Pattern(
        regexp = "[A-Za-z0-9_-]{43}",
        message = "이메일 인증 토큰 형식이 올바르지 않습니다."
    )
    String emailVerificationToken
) {

  public SignupRequest {
    name = stripNullable(name);
    studentNumber = stripNullable(studentNumber);
    email = normalizeEmail(email);
  }

  @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
  public boolean isPasswordConfirmed() {
    return password != null && password.equals(passwordConfirm);
  }

  private static String stripNullable(String value) {
    return value == null ? null : value.strip();
  }

  private static String normalizeEmail(String email) {
    String stripped = stripNullable(email);
    return stripped == null ? null : stripped.toLowerCase(Locale.ROOT);
  }
}
