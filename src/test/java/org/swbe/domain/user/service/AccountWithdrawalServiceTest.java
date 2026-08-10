package org.swbe.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.swbe.domain.user.entity.AccountStatus;
import org.swbe.domain.user.entity.AppRole;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.entity.UserRole;
import org.swbe.domain.user.exception.AuthErrorCode;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.domain.user.repository.UserRoleRepository;
import org.swbe.global.error.BusinessException;

class AccountWithdrawalServiceTest {

  private static final Long USER_ID = 10L;
  private static final LocalDateTime NOW =
      LocalDateTime.of(2026, 8, 1, 12, 0);

  private AppUserRepository appUserRepository;
  private UserRoleRepository userRoleRepository;
  private AccountWithdrawalService service;

  @BeforeEach
  void setUp() {
    appUserRepository = mock(AppUserRepository.class);
    userRoleRepository = mock(UserRoleRepository.class);
    Clock clock = Clock.fixed(
        Instant.parse("2026-08-01T12:00:00Z"),
        ZoneOffset.UTC
    );
    service = new AccountWithdrawalService(
        appUserRepository,
        userRoleRepository,
        clock
    );
  }

  @Test
  void withdrawRevokesActiveRolesAndAnonymizesUser() {
    AppUser user = user();
    UserRole firstRole = role(user);
    UserRole secondRole = role(user);
    when(appUserRepository.findByIdForUpdate(USER_ID))
        .thenReturn(Optional.of(user));
    when(userRoleRepository.findActiveByUserIdForUpdate(USER_ID))
        .thenReturn(List.of(firstRole, secondRole));

    service.withdraw(USER_ID);

    assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.WITHDRAWN);
    assertThat(user.getEmail()).isEqualTo("withdrawn-10@users.invalid");
    assertThat(firstRole.getRevokedAt()).isEqualTo(NOW);
    assertThat(secondRole.getRevokedAt()).isEqualTo(NOW);
  }

  @Test
  void missingAccountReturnsBusinessError() {
    when(appUserRepository.findByIdForUpdate(USER_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.withdraw(USER_ID))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.ACCOUNT_NOT_FOUND)
        );
  }

  private AppUser user() {
    AppUser user = AppUser.registerStudent(
        "student@mju.ac.kr",
        "{bcrypt}encoded-password",
        "홍길동",
        "60241234",
        NOW.minusDays(1)
    );
    ReflectionTestUtils.setField(user, "id", USER_ID);
    return user;
  }

  private UserRole role(AppUser user) {
    return UserRole.grantBySystem(
        user,
        mock(AppRole.class),
        NOW.minusDays(1)
    );
  }
}
