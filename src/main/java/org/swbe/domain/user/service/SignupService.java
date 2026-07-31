package org.swbe.domain.user.service;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.user.dto.request.SignupRequest;
import org.swbe.domain.user.dto.response.SignupResponse;
import org.swbe.domain.user.entity.AppRole;
import org.swbe.domain.user.entity.AppRoleCode;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.entity.EmailVerification;
import org.swbe.domain.user.entity.UserRole;
import org.swbe.domain.user.exception.AuthErrorCode;
import org.swbe.domain.user.exception.SignupVerificationException;
import org.swbe.domain.user.repository.AppRoleRepository;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.domain.user.repository.EmailVerificationRepository;
import org.swbe.domain.user.repository.UserRoleRepository;
import org.swbe.global.error.BusinessException;

@Service
@RequiredArgsConstructor
public class SignupService {

  private final AppUserRepository appUserRepository;
  private final AppRoleRepository appRoleRepository;
  private final UserRoleRepository userRoleRepository;
  private final EmailVerificationRepository verificationRepository;
  private final VerificationValueGenerator valueGenerator;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  @Transactional
  public SignupResponse signup(SignupRequest request) {
    LocalDateTime now = LocalDateTime.now(clock);
    EmailVerification verification = findVerification(
        request.emailVerificationToken()
    );
    verification.validateUsableForSignup(request.email(), now);

    validateDuplicateIdentifiers(
        request.email(),
        request.studentNumber()
    );
    AppRoleCode roleCode = AppRoleCode.STUDENT;
    AppRole role = findRole(roleCode);

    AppUser user = AppUser.registerStudent(
        request.email(),
        passwordEncoder.encode(request.password()),
        request.name(),
        request.studentNumber(),
        now
    );
    saveUser(user);
    userRoleRepository.save(UserRole.grantBySystem(user, role, now));
    verification.consumeForSignup(user, request.email(), now);

    return SignupResponse.from(user, roleCode);
  }

  private EmailVerification findVerification(String rawToken) {
    String tokenHash = valueGenerator.hashToken(rawToken);
    return verificationRepository.findByVerificationTokenHash(tokenHash)
        .orElseThrow(SignupVerificationException::invalidToken);
  }

  private void validateDuplicateIdentifiers(
      String email,
      String studentNumber
  ) {
    if (appUserRepository.existsByEmailIgnoreCase(email)) {
      throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
    }
    if (appUserRepository.existsByStudentNumber(studentNumber)) {
      throw new BusinessException(
          AuthErrorCode.STUDENT_NUMBER_ALREADY_REGISTERED
      );
    }
  }

  private AppRole findRole(AppRoleCode roleCode) {
    return appRoleRepository.findByCode(roleCode.name())
        .orElseThrow(() -> new IllegalStateException(
            roleCode.name() + " role is not configured"
        ));
  }

  private void saveUser(AppUser user) {
    try {
      appUserRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(AuthErrorCode.SIGNUP_CONFLICT);
    }
  }
}
