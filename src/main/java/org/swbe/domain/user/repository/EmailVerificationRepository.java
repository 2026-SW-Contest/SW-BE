package org.swbe.domain.user.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.swbe.domain.user.entity.EmailVerification;
import org.swbe.domain.user.entity.EmailVerificationPurpose;

public interface EmailVerificationRepository
    extends JpaRepository<EmailVerification, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<EmailVerification>
      findFirstByEmailAndPurposeOrderByCreatedAtDescIdDesc(
          String email,
          EmailVerificationPurpose purpose
      );

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<EmailVerification> findByVerificationTokenHash(
      String verificationTokenHash
  );
}
