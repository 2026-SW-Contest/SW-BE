package org.swbe.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VerificationValueGeneratorTest {

  private final VerificationValueGenerator generator =
      new VerificationValueGenerator();

  @Test
  void generatesSixDigitCode() {
    String code = generator.generateCode();

    assertThat(code).matches("\\d{6}");
  }

  @Test
  void generatesUrlSafeRandomToken() {
    String firstToken = generator.generateToken();
    String secondToken = generator.generateToken();

    assertThat(firstToken).matches("[A-Za-z0-9_-]{43}");
    assertThat(secondToken).isNotEqualTo(firstToken);
  }

  @Test
  void hashesTokenWithSha256() {
    String tokenHash = generator.hashToken("verification-token");

    assertThat(tokenHash)
        .hasSize(64)
        .matches("[0-9a-f]{64}")
        .isEqualTo(generator.hashToken("verification-token"));
  }
}
