package org.swbe.domain.user.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class VerificationValueGenerator {

  private static final int CODE_BOUND = 1_000_000;
  private static final int TOKEN_BYTE_LENGTH = 32;
  private static final String HASH_ALGORITHM = "SHA-256";

  private final SecureRandom secureRandom = new SecureRandom();

  public String generateCode() {
    return "%06d".formatted(secureRandom.nextInt(CODE_BOUND));
  }

  public String generateToken() {
    byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
    secureRandom.nextBytes(tokenBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
  }

  public String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 algorithm is not available", exception);
    }
  }
}
