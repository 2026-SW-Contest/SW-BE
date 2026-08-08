package org.swbe.domain.user.service;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.search.repository.RecentSearchRepository;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.exception.AuthErrorCode;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.domain.user.repository.UserRoleRepository;
import org.swbe.global.error.BusinessException;

@Service
@RequiredArgsConstructor
public class AccountWithdrawalService {

  private final AppUserRepository appUserRepository;
  private final UserRoleRepository userRoleRepository;
  private final RecentSearchRepository recentSearchRepository;
  private final Clock clock;

  @Transactional
  public void withdraw(Long userId) {
    AppUser user = appUserRepository.findByIdForUpdate(userId)
        .orElseThrow(() -> new BusinessException(
            AuthErrorCode.ACCOUNT_NOT_FOUND
        ));
    LocalDateTime now = LocalDateTime.now(clock);

    userRoleRepository.findActiveByUserIdForUpdate(userId)
        .forEach(userRole -> userRole.revoke(now));
    recentSearchRepository.deleteAllByUserId(userId);
    user.withdraw(now);
  }
}
