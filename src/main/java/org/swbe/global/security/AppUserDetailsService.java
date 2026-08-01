package org.swbe.global.security;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.domain.user.repository.UserRoleRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppUserDetailsService implements UserDetailsService {

  private final AppUserRepository appUserRepository;
  private final UserRoleRepository userRoleRepository;

  @Override
  public UserDetails loadUserByUsername(String email) {
    String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);

    AppUser user = appUserRepository.findByEmailIgnoreCase(normalizedEmail)
        .filter(candidate -> candidate.getPasswordHash() != null)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    List<SimpleGrantedAuthority> authorities =
        userRoleRepository.findActiveRoleCodesByUserId(user.getId()).stream()
            .map(roleCode -> new SimpleGrantedAuthority("ROLE_" + roleCode))
            .toList();

    return new AppUserPrincipal(
        user.getId(),
        user.getEmail(),
        user.getPasswordHash(),
        user.getAccountStatus(),
        user.isEmailVerified(),
        authorities
    );
  }
}
