package org.swbe.domain.user.dto.response;

import java.util.List;

public record LoginResponse(
    Long userId,
    String email,
    List<String> roles
) {

  public LoginResponse {
    roles = List.copyOf(roles);
  }
}
