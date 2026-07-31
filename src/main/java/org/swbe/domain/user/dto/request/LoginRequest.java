package org.swbe.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이어야 합니다.")
    @Size(max = 255, message = "이메일은 255자를 초과할 수 없습니다.")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(max = 100, message = "비밀번호는 100자를 초과할 수 없습니다.")
    String password
) {
}
