package org.swbe.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.swbe.domain.user.dto.request.PasswordChangeRequest;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.exception.PasswordChangeErrorCode;
import org.swbe.domain.user.exception.UserErrorCode;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.global.error.BusinessException;

class PasswordChangeServiceTest {

  private static final LocalDateTime CHANGED_AT =
      LocalDateTime.of(2026, 8, 12, 18, 0);

  private AppUserRepository appUserRepository;
  private PasswordEncoder passwordEncoder;
  private PasswordChangeService service;

  @BeforeEach
  void setUp() {
    appUserRepository = mock(AppUserRepository.class);
    passwordEncoder = PasswordEncoderFactories
        .createDelegatingPasswordEncoder();
    Clock clock = Clock.fixed(
        Instant.parse("2026-08-12T18:00:00Z"),
        ZoneOffset.UTC
    );
    service = new PasswordChangeService(
        appUserRepository,
        passwordEncoder,
        clock
    );
  }

  @Test
  void changesPasswordToEncodedValue() {
    AppUser user = user("Current12!@");
    when(appUserRepository.findByIdForUpdate(7L))
        .thenReturn(Optional.of(user));

    service.changePassword(
        7L,
        request("Current12!@", "Changed34#$", "Changed34#$")
    );

    assertThat(passwordEncoder.matches(
        "Changed34#$",
        user.getPasswordHash()
    )).isTrue();
    assertThat(user.getUpdatedAt()).isEqualTo(CHANGED_AT);
  }

  @Test
  void wrongCurrentPasswordIsRejected() {
    AppUser user = user("Current12!@");
    when(appUserRepository.findByIdForUpdate(7L))
        .thenReturn(Optional.of(user));

    assertThatThrownBy(() -> service.changePassword(
        7L,
        request("Wrong123!@", "Changed34#$", "Changed34#$")
    )).isInstanceOfSatisfying(BusinessException.class, exception ->
        assertThat(exception.getErrorCode()).isEqualTo(
            PasswordChangeErrorCode.CURRENT_PASSWORD_MISMATCH
        )
    );
  }

  @Test
  void passwordConfirmationMismatchIsRejected() {
    AppUser user = user("Current12!@");
    when(appUserRepository.findByIdForUpdate(7L))
        .thenReturn(Optional.of(user));

    assertThatThrownBy(() -> service.changePassword(
        7L,
        request("Current12!@", "Changed34#$", "Different56!@")
    )).isInstanceOfSatisfying(BusinessException.class, exception ->
        assertThat(exception.getErrorCode()).isEqualTo(
            PasswordChangeErrorCode.PASSWORD_CONFIRMATION_MISMATCH
        )
    );
  }

  @Test
  void currentPasswordCannotBeReused() {
    AppUser user = user("Current12!@");
    when(appUserRepository.findByIdForUpdate(7L))
        .thenReturn(Optional.of(user));

    assertThatThrownBy(() -> service.changePassword(
        7L,
        request("Current12!@", "Current12!@", "Current12!@")
    )).isInstanceOfSatisfying(BusinessException.class, exception ->
        assertThat(exception.getErrorCode()).isEqualTo(
            PasswordChangeErrorCode.PASSWORD_REUSE_NOT_ALLOWED
        )
    );
  }

  @Test
  void missingUserIsRejected() {
    when(appUserRepository.findByIdForUpdate(99L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.changePassword(
        99L,
        request("Current12!@", "Changed34#$", "Changed34#$")
    )).isInstanceOfSatisfying(BusinessException.class, exception ->
        assertThat(exception.getErrorCode())
            .isEqualTo(UserErrorCode.NOT_FOUND)
    );
  }

  private AppUser user(String password) {
    AppUser user = AppUser.registerStudent(
        "student@mju.ac.kr",
        passwordEncoder.encode(password),
        "홍길동",
        "60241234",
        LocalDateTime.of(2026, 8, 1, 12, 0)
    );
    ReflectionTestUtils.setField(user, "id", 7L);
    return user;
  }

  private PasswordChangeRequest request(
      String currentPassword,
      String newPassword,
      String newPasswordConfirm
  ) {
    return new PasswordChangeRequest(
        currentPassword,
        newPassword,
        newPasswordConfirm
    );
  }
}
