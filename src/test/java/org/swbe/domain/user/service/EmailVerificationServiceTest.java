package org.swbe.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.swbe.domain.user.config.EmailVerificationProperties;
import org.swbe.domain.user.dto.request.EmailVerificationConfirmRequest;
import org.swbe.domain.user.dto.request.EmailVerificationSendRequest;
import org.swbe.domain.user.dto.response.EmailVerificationTokenResponse;
import org.swbe.domain.user.entity.EmailVerification;
import org.swbe.domain.user.entity.EmailVerificationPurpose;
import org.swbe.domain.user.exception.AuthErrorCode;
import org.swbe.domain.user.exception.VerificationCodeMismatchException;
import org.swbe.domain.user.exception.VerificationEmailSendException;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.domain.user.repository.EmailVerificationRepository;
import org.swbe.global.error.BusinessException;

class EmailVerificationServiceTest {

  private static final String EMAIL = "student@mju.ac.kr";
  private static final String CODE = "012345";
  private static final LocalDateTime NOW =
      LocalDateTime.of(2026, 7, 31, 12, 0);

  private AppUserRepository appUserRepository;
  private EmailVerificationRepository verificationRepository;
  private VerificationEmailSender emailSender;
  private VerificationValueGenerator valueGenerator;
  private PasswordEncoder passwordEncoder;
  private EmailVerificationService service;

  @BeforeEach
  void setUp() {
    appUserRepository = mock(AppUserRepository.class);
    verificationRepository = mock(EmailVerificationRepository.class);
    emailSender = mock(VerificationEmailSender.class);
    valueGenerator = mock(VerificationValueGenerator.class);
    passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    EmailVerificationProperties properties =
        new EmailVerificationProperties(
            Duration.ofMinutes(5),
            Duration.ofMinutes(1),
            5,
            Duration.ofMinutes(30)
        );
    Clock clock = Clock.fixed(
        Instant.parse("2026-07-31T12:00:00Z"),
        ZoneOffset.UTC
    );
    service = new EmailVerificationService(
        appUserRepository,
        verificationRepository,
        emailSender,
        valueGenerator,
        passwordEncoder,
        properties,
        clock
    );

    when(verificationRepository
        .findFirstByEmailAndPurposeOrderByCreatedAtDescIdDesc(
            EMAIL,
            EmailVerificationPurpose.SIGNUP
        ))
        .thenReturn(Optional.empty());
    when(valueGenerator.generateCode()).thenReturn(CODE);
  }

  @Test
  void storesHashedCodeAndSendsRawCode() {
    service.sendVerificationCode(
        new EmailVerificationSendRequest("STUDENT@MJU.AC.KR")
    );

    ArgumentCaptor<EmailVerification> verificationCaptor =
        ArgumentCaptor.forClass(EmailVerification.class);
    verify(verificationRepository).save(verificationCaptor.capture());
    EmailVerification saved = verificationCaptor.getValue();

    assertThat(saved.getEmail()).isEqualTo(EMAIL);
    assertThat(saved.getCodeHash()).isNotEqualTo(CODE);
    assertThat(passwordEncoder.matches(CODE, saved.getCodeHash())).isTrue();
    assertThat(saved.getExpiresAt()).isEqualTo(NOW.plusMinutes(5));
    verify(emailSender).sendVerificationCode(
        EMAIL,
        CODE,
        Duration.ofMinutes(5)
    );
  }

  @Test
  void registeredEmailCannotRequestCode() {
    when(appUserRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

    assertBusinessError(
        () -> service.sendVerificationCode(
            new EmailVerificationSendRequest(EMAIL)
        ),
        AuthErrorCode.EMAIL_ALREADY_REGISTERED
    );

    verify(verificationRepository, never()).save(any());
    verify(emailSender, never()).sendVerificationCode(any(), any(), any());
  }

  @Test
  void resendBeforeCooldownIsRejected() {
    EmailVerification previous = createVerification(
        NOW.minusSeconds(59),
        CODE
    );
    findLatestReturns(previous);

    assertBusinessError(
        () -> service.sendVerificationCode(
            new EmailVerificationSendRequest(EMAIL)
        ),
        AuthErrorCode.EMAIL_VERIFICATION_RESEND_TOO_SOON
    );

    verify(verificationRepository, never()).save(any());
  }

  @Test
  void resendAfterCooldownInvalidatesPreviousRequest() {
    EmailVerification previous = createVerification(
        NOW.minusMinutes(1),
        CODE
    );
    previous.completeVerification(
        "old-token-hash",
        NOW.minusSeconds(30),
        NOW.plusMinutes(29)
    );
    findLatestReturns(previous);

    service.sendVerificationCode(new EmailVerificationSendRequest(EMAIL));

    assertThat(previous.getExpiresAt()).isEqualTo(NOW);
    assertThat(previous.getVerificationTokenHash()).isNull();
    assertThat(previous.getVerificationTokenExpiresAt()).isNull();
    verify(verificationRepository).save(any(EmailVerification.class));
  }

  @Test
  void emailDeliveryFailureIsConvertedToBusinessError() {
    org.mockito.Mockito.doThrow(new VerificationEmailSendException(
            new IllegalStateException("SMTP unavailable")
        ))
        .when(emailSender)
        .sendVerificationCode(any(), any(), any());

    assertBusinessError(
        () -> service.sendVerificationCode(
            new EmailVerificationSendRequest(EMAIL)
        ),
        AuthErrorCode.EMAIL_SEND_FAILED
    );
  }

  @Test
  void correctCodeCompletesVerificationAndReturnsRawToken() {
    EmailVerification verification = createVerification(NOW, CODE);
    findLatestReturns(verification);
    when(valueGenerator.generateToken()).thenReturn("signup-token");
    when(valueGenerator.hashToken("signup-token"))
        .thenReturn("token-hash");

    EmailVerificationTokenResponse response =
        service.confirmVerificationCode(
            new EmailVerificationConfirmRequest(EMAIL, CODE)
        );

    assertThat(response.emailVerificationToken()).isEqualTo("signup-token");
    assertThat(response.expiresAt()).isEqualTo(NOW.plusMinutes(30));
    assertThat(verification.getVerificationTokenHash())
        .isEqualTo("token-hash");
    assertThat(verification.getVerificationTokenHash())
        .isNotEqualTo(response.emailVerificationToken());
    assertThat(verification.getVerifiedAt()).isEqualTo(NOW);
  }

  @Test
  void wrongCodeIncrementsAttemptCount() {
    EmailVerification verification = createVerification(NOW, CODE);
    findLatestReturns(verification);

    assertThatThrownBy(() -> service.confirmVerificationCode(
        new EmailVerificationConfirmRequest(EMAIL, "999999")
    )).isInstanceOf(VerificationCodeMismatchException.class);

    assertThat(verification.getAttemptCount()).isEqualTo(1);
  }

  @Test
  void expiredCodeCannotBeConfirmed() {
    EmailVerification verification = createVerification(
        NOW.minusMinutes(5),
        CODE
    );
    findLatestReturns(verification);

    assertBusinessError(
        () -> service.confirmVerificationCode(
            new EmailVerificationConfirmRequest(EMAIL, CODE)
        ),
        AuthErrorCode.EMAIL_VERIFICATION_EXPIRED
    );
  }

  @Test
  void codeCannotBeConfirmedAfterFiveFailures() {
    EmailVerification verification = createVerification(NOW, CODE);
    for (int attempt = 0; attempt < 5; attempt++) {
      verification.recordFailedAttempt();
    }
    findLatestReturns(verification);

    assertBusinessError(
        () -> service.confirmVerificationCode(
            new EmailVerificationConfirmRequest(EMAIL, CODE)
        ),
        AuthErrorCode.EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED
    );
  }

  private EmailVerification createVerification(
      LocalDateTime createdAt,
      String rawCode
  ) {
    return EmailVerification.createSignupVerification(
        EMAIL,
        passwordEncoder.encode(rawCode),
        createdAt.plusMinutes(5),
        createdAt
    );
  }

  private void findLatestReturns(EmailVerification verification) {
    when(verificationRepository
        .findFirstByEmailAndPurposeOrderByCreatedAtDescIdDesc(
            EMAIL,
            EmailVerificationPurpose.SIGNUP
        ))
        .thenReturn(Optional.of(verification));
  }

  private void assertBusinessError(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
      AuthErrorCode expectedErrorCode
  ) {
    assertThatThrownBy(callable)
        .isInstanceOf(BusinessException.class)
        .extracting(exception ->
            ((BusinessException) exception).getErrorCode()
        )
        .isEqualTo(expectedErrorCode);
  }
}
