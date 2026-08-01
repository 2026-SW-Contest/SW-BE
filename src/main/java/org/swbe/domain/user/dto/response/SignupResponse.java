package org.swbe.domain.user.dto.response;

import java.util.List;
import org.swbe.domain.user.entity.AppRoleCode;
import org.swbe.domain.user.entity.AppUser;

public record SignupResponse(
    Long userId,
    String email,
    String name,
    String studentNumber,
    List<String> roles
) {

  public SignupResponse {
    roles = List.copyOf(roles);
  }

  public static SignupResponse from(
      AppUser user,
      AppRoleCode roleCode
  ) {
    return new SignupResponse(
        user.getId(),
        user.getEmail(),
        user.getName(),
        user.getStudentNumber(),
        List.of(roleCode.name())
    );
  }
}
