package org.swbe.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserRoleTest {

  @Test
  void grantsRoleBySystemWithoutGrantingUser() {
    AppUser user = mock(AppUser.class);
    AppRole role = mock(AppRole.class);
    LocalDateTime grantedAt = LocalDateTime.of(2026, 7, 31, 12, 0);

    UserRole userRole = UserRole.grantBySystem(user, role, grantedAt);

    assertThat(userRole.getUser()).isSameAs(user);
    assertThat(userRole.getRole()).isSameAs(role);
    assertThat(userRole.getGrantedBy()).isNull();
    assertThat(userRole.getGrantedAt()).isEqualTo(grantedAt);
    assertThat(userRole.getRevokedAt()).isNull();
  }
}
