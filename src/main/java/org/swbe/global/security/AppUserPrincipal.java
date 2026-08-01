package org.swbe.global.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.swbe.domain.user.entity.AccountStatus;

public final class AppUserPrincipal
    implements UserDetails, CredentialsContainer, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Long userId;
  private final String email;
  private final AccountStatus accountStatus;
  private final boolean emailVerified;
  private final List<? extends GrantedAuthority> authorities;
  private String passwordHash;

  public AppUserPrincipal(
      Long userId,
      String email,
      String passwordHash,
      AccountStatus accountStatus,
      boolean emailVerified,
      List<? extends GrantedAuthority> authorities
  ) {
    this.userId = userId;
    this.email = email;
    this.passwordHash = passwordHash;
    this.accountStatus = accountStatus;
    this.emailVerified = emailVerified;
    this.authorities = List.copyOf(authorities);
  }

  public Long getUserId() {
    return userId;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return accountStatus != AccountStatus.SUSPENDED;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return accountStatus == AccountStatus.ACTIVE && emailVerified;
  }

  @Override
  public void eraseCredentials() {
    passwordHash = null;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof AppUserPrincipal other)) {
      return false;
    }
    return Objects.equals(userId, other.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId);
  }
}
