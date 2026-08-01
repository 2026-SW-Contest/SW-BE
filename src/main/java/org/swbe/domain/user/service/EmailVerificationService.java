package org.swbe.domain.user.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private final AppUserRepository appUserRepository;
  private final EmailVerificationRepository verificationRepository;
  private final VerificationEmailSender emailSender;
  private final VerificationValueGenerator valueGenerator;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationProperties properties;
  private final Clock clock;

  @Transactional
  public void sendVerificationCode(EmailVerificationSendRequest request) {
    String email = normalizeEmail(request.email());
    if (appUserRepository.existsByEmailIgnoreCase(email)) {
      throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
    }

    LocalDateTime now = LocalDateTime.now(clock);
    verificationRepository
        .findFirstByEmailAndPurposeOrderByCreatedAtDescIdDesc(
            email,
            EmailVerificationPurpose.SIGNUP
        )
        .ifPresent(previous -> invalidatePrevious(previous, now));

    String code = valueGenerator.generateCode();
    EmailVerification verification =
        EmailVerification.createSignupVerification(
            email,
            passwordEncoder.encode(code),
            now.plus(properties.codeValidity()),
            now
        );
    verificationRepository.save(verification);

    try {
      emailSender.sendVerificationCode(
          email,
          code,
          properties.codeValidity()
      );
    } catch (VerificationEmailSendException exception) {
      throw new BusinessException(AuthErrorCode.EMAIL_SEND_FAILED);
    }
  }

  @Transactional(noRollbackFor = VerificationCodeMismatchException.class)
  public EmailVerificationTokenResponse confirmVerificationCode(
      EmailVerificationConfirmRequest request
  ) {
    String email = normalizeEmail(request.email());
    EmailVerification verification = verificationRepository
        .findFirstByEmailAndPurposeOrderByCreatedAtDescIdDesc(
            email,
            EmailVerificationPurpose.SIGNUP
        )
        .orElseThrow(() -> new BusinessException(
            AuthErrorCode.EMAIL_VERIFICATION_NOT_FOUND
        ));
    LocalDateTime now = LocalDateTime.now(clock);

    validateConfirmable(verification, now);
    if (!passwordEncoder.matches(request.code(), verification.getCodeHash())) {
      verification.recordFailedAttempt();
      throw new VerificationCodeMismatchException();
    }

    String token = valueGenerator.generateToken();
    LocalDateTime tokenExpiresAt = now.plus(properties.tokenValidity());
    verification.completeVerification(
        valueGenerator.hashToken(token),
        now,
        tokenExpiresAt
    );

    return new EmailVerificationTokenResponse(token, tokenExpiresAt);
  }

  private void invalidatePrevious(
      EmailVerification previous,
      LocalDateTime now
  ) {
    if (!previous.isResendAllowed(now, properties.resendCooldown())) {
      throw new BusinessException(
          AuthErrorCode.EMAIL_VERIFICATION_RESEND_TOO_SOON
      );
    }
    previous.invalidate(now);
  }

  private void validateConfirmable(
      EmailVerification verification,
      LocalDateTime now
  ) {
    if (verification.isVerified()) {
      throw new BusinessException(
          AuthErrorCode.EMAIL_VERIFICATION_ALREADY_COMPLETED
      );
    }
    if (verification.isCodeExpired(now)) {
      throw new BusinessException(AuthErrorCode.EMAIL_VERIFICATION_EXPIRED);
    }
    if (verification.hasReachedAttemptLimit(properties.maxAttempts())) {
      throw new BusinessException(
          AuthErrorCode.EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED
      );
    }
  }

  private String normalizeEmail(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }
}
