package org.swbe.domain.user.repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.swbe.domain.user.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

  Optional<AppUser> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByStudentNumber(String studentNumber);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select user from AppUser user where user.id = :userId")
  Optional<AppUser> findByIdForUpdate(@Param("userId") Long userId);
}
