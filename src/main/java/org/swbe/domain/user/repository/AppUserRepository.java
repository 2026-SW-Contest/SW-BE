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

  // 현재 사용자 응답에 필요한 소속 부서를 사용자와 함께 조회한다.
  @Query("""
      select user
      from AppUser user
      left join fetch user.department
      where user.id = :userId
      """)
  Optional<AppUser> findByIdWithDepartment(@Param("userId") Long userId);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByStudentNumber(String studentNumber);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select user from AppUser user where user.id = :userId")
  Optional<AppUser> findByIdForUpdate(@Param("userId") Long userId);
}
