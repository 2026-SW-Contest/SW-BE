package org.swbe.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerificationSendRequest(
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Pattern(
        regexp = "(?i)^[A-Z0-9._%+-]+@mju\\.ac\\.kr$",
        message = "mju.ac.kr 이메일만 사용할 수 있습니다."
    )
    String email
) {

}
