package org.swbe.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.swbe.domain.user.dto.request.SignupRequest;
import org.swbe.domain.user.dto.response.SignupResponse;
import org.swbe.domain.user.entity.AccountStatus;
import org.swbe.domain.user.entity.AppRole;
import org.swbe.domain.user.entity.AppRoleCode;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.entity.EmailVerification;
import org.swbe.domain.user.entity.UserRole;
import org.swbe.domain.user.exception.AuthErrorCode;
import org.swbe.domain.user.repository.AppRoleRepository;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.domain.user.repository.EmailVerificationRepository;
import org.swbe.domain.user.repository.UserRoleRepository;
import org.swbe.global.error.BusinessException;

class SignupServiceTest {

  private static final String EMAIL = "student@mju.ac.kr";
  private static final String STUDENT_NUMBER = "60241234";
  private static final String RAW_PASSWORD = "password1";
  private static final String RAW_TOKEN = "a".repeat(43);
  private static final String TOKEN_HASH = "token-hash";
  private static final LocalDateTime NOW =
      LocalDateTime.of(2026, 7, 31, 12, 0);

  private AppUserRepository appUserRepository;
  private AppRoleRepository appRoleRepository;
  private UserRoleRepository userRoleRepository;
  private EmailVerificationRepository verificationRepository;
  private VerificationValueGenerator valueGenerator;
  private PasswordEncoder passwordEncoder;
  private AppRole studentRole;
  private EmailVerification verification;
  private SignupService signupService;

  @BeforeEach
  void setUp() {
    appUserRepository = mock(AppUserRepository.class);
    appRoleRepository = mock(AppRoleRepository.class);
    userRoleRepository = mock(UserRoleRepository.class);
    verificationRepository = mock(EmailVerificationRepository.class);
    valueGenerator = mock(VerificationValueGenerator.class);
    passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    studentRole = mock(AppRole.class);
    verification = verifiedEmail();
    Clock clock = Clock.fixed(
        Instant.parse("2026-07-31T12:00:00Z"),
        ZoneOffset.UTC
    );
    signupService = new SignupService(
        appUserRepository,
        appRoleRepository,
        userRoleRepository,
        verificationRepository,
        valueGenerator,
        passwordEncoder,
        clock
    );

    when(valueGenerator.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
    when(verificationRepository.findByVerificationTokenHash(TOKEN_HASH))
        .thenReturn(Optional.of(verification));
    when(appRoleRepository.findByCode(AppRoleCode.STUDENT.name()))
        .thenReturn(Optional.of(studentRole));
    when(appUserRepository.saveAndFlush(any(AppUser.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void signupCreatesActiveStudentAndConsumesVerification() {
    SignupResponse response = signupService.signup(signupRequest());

    ArgumentCaptor<AppUser> userCaptor =
        ArgumentCaptor.forClass(AppUser.class);
    verify(appUserRepository).saveAndFlush(userCaptor.capture());
    AppUser savedUser = userCaptor.getValue();

    assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
    assertThat(savedUser.getName()).isEqualTo("홍길동");
    assertThat(savedUser.getStudentNumber()).isEqualTo(STUDENT_NUMBER);
    assertThat(savedUser.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
    assertThat(passwordEncoder.matches(
        RAW_PASSWORD,
        savedUser.getPasswordHash()
    )).isTrue();
    assertThat(savedUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(savedUser.isEmailVerified()).isTrue();

    ArgumentCaptor<UserRole> userRoleCaptor =
        ArgumentCaptor.forClass(UserRole.class);
    verify(userRoleRepository).save(userRoleCaptor.capture());
    assertThat(userRoleCaptor.getValue().getUser()).isSameAs(savedUser);
    assertThat(userRoleCaptor.getValue().getRole()).isSameAs(studentRole);
    assertThat(verification.getUser()).isSameAs(savedUser);
    assertThat(verification.getConsumedAt()).isEqualTo(NOW);
    assertThat(response.roles()).containsExactly("STUDENT");
  }

  @Test
  void invalidTokenCannotCreateUser() {
    when(verificationRepository.findByVerificationTokenHash(TOKEN_HASH))
        .thenReturn(Optional.empty());

    assertBusinessError(
        () -> signupService.signup(signupRequest()),
        AuthErrorCode.INVALID_EMAIL_VERIFICATION_TOKEN
    );
  }

  @Test
  void duplicateEmailCannotCreateUser() {
    when(appUserRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

    assertBusinessError(
        () -> signupService.signup(signupRequest()),
        AuthErrorCode.EMAIL_ALREADY_REGISTERED
    );
  }

  @Test
  void duplicateStudentNumberCannotCreateUser() {
    when(appUserRepository.existsByStudentNumber(STUDENT_NUMBER))
        .thenReturn(true);

    assertBusinessError(
        () -> signupService.signup(signupRequest()),
        AuthErrorCode.STUDENT_NUMBER_ALREADY_REGISTERED
    );
  }

  @Test
  void databaseUniqueConstraintConflictReturnsConflictError() {
    when(appUserRepository.saveAndFlush(any(AppUser.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate"));

    assertBusinessError(
        () -> signupService.signup(signupRequest()),
        AuthErrorCode.SIGNUP_CONFLICT
    );
  }

  private EmailVerification verifiedEmail() {
    EmailVerification result =
        EmailVerification.createSignupVerification(
            EMAIL,
            "{bcrypt}code-hash",
            NOW.plusMinutes(5),
            NOW.minusMinutes(1)
        );
    result.completeVerification(
        TOKEN_HASH,
        NOW.minusSeconds(30),
        NOW.plusMinutes(30)
    );
    return result;
  }

  private SignupRequest signupRequest() {
    return new SignupRequest(
        "홍길동",
        STUDENT_NUMBER,
        EMAIL,
        RAW_PASSWORD,
        RAW_PASSWORD,
        RAW_TOKEN
    );
  }

  private void assertBusinessError(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
      AuthErrorCode expectedErrorCode
  ) {
    assertThatThrownBy(callable)
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(expectedErrorCode)
        );
  }
}
