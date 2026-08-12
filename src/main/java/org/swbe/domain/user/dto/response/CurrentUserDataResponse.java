package org.swbe.domain.user.dto.response;

import java.util.List;

public record CurrentUserDataResponse(
    Long userId,
    String name,
    String email,
    String studentNumber,
    CurrentUserDepartmentResponse department,
    List<String> roles
) {

  public CurrentUserDataResponse {
    roles = List.copyOf(roles);
  }
}
