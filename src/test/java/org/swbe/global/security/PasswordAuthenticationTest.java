package org.swbe.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordAuthenticationTest {

  private AuthenticationManager authenticationManager;

  @BeforeEach
  void setUp() {
    PasswordEncoder passwordEncoder =
        PasswordEncoderFactories.createDelegatingPasswordEncoder();
    String storedPasswordHash = passwordEncoder.encode("correct-password");
    UserDetailsService userDetailsService = username ->
        User.withUsername(username)
            .password(storedPasswordHash)
            .roles("STUDENT")
            .build();

    authenticationManager = new SecurityConfig().authenticationManager(
        userDetailsService,
        passwordEncoder
    );
  }

  @Test
  void rawPasswordMatchingStoredHashAuthenticatesSuccessfully() {
    Authentication authentication = authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken.unauthenticated(
            "student@example.com",
            "correct-password"
        )
    );

    assertThat(authentication.isAuthenticated()).isTrue();
    assertThat(authentication.getName()).isEqualTo("student@example.com");
  }

  @Test
  void rawPasswordNotMatchingStoredHashFailsAuthentication() {
    Authentication authentication =
        UsernamePasswordAuthenticationToken.unauthenticated(
            "student@example.com",
            "wrong-password"
        );

    assertThatThrownBy(() -> authenticationManager.authenticate(authentication))
        .isInstanceOf(BadCredentialsException.class);
  }
}
