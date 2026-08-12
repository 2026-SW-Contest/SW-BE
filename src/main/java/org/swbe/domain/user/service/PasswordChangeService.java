package org.swbe.domain.user.service;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.user.dto.request.PasswordChangeRequest;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.exception.PasswordChangeErrorCode;
import org.swbe.domain.user.exception.UserErrorCode;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.global.error.BusinessException;

@Service
public class PasswordChangeService {

  private final AppUserRepository appUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  public PasswordChangeService(
      AppUserRepository appUserRepository,
      PasswordEncoder passwordEncoder,
      Clock clock
  ) {
    this.appUserRepository = appUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  // 현재 비밀번호와 새 비밀번호를 검증한 뒤 암호화된 값으로 변경한다.
  @Transactional
  public void changePassword(
      Long userId,
      PasswordChangeRequest request
  ) {
    AppUser user = appUserRepository.findByIdForUpdate(userId)
        .orElseThrow(() -> new BusinessException(
            UserErrorCode.NOT_FOUND
        ));
    validateCurrentPassword(user, request.currentPassword());
    validatePasswordConfirmation(request);
    validatePasswordNotReused(user, request.newPassword());

    String encodedPassword = passwordEncoder.encode(
        request.newPassword()
    );
    user.changePasswordHash(
        encodedPassword,
        LocalDateTime.now(clock)
    );
  }

  // 저장된 해시와 사용자가 입력한 현재 비밀번호가 일치하는지 확인한다.
  private void validateCurrentPassword(
      AppUser user,
      String currentPassword
  ) {
    if (!passwordEncoder.matches(
        currentPassword,
        user.getPasswordHash()
    )) {
      throw new BusinessException(
          PasswordChangeErrorCode.CURRENT_PASSWORD_MISMATCH
      );
    }
  }

  // 새 비밀번호와 확인값이 정확히 일치하는지 확인한다.
  private void validatePasswordConfirmation(
      PasswordChangeRequest request
  ) {
    if (!request.newPassword().equals(request.newPasswordConfirm())) {
      throw new BusinessException(
          PasswordChangeErrorCode.PASSWORD_CONFIRMATION_MISMATCH
      );
    }
  }

  // 현재 비밀번호를 새 비밀번호로 다시 사용하는 것을 막는다.
  private void validatePasswordNotReused(
      AppUser user,
      String newPassword
  ) {
    if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
      throw new BusinessException(
          PasswordChangeErrorCode.PASSWORD_REUSE_NOT_ALLOWED
      );
    }
  }
}
