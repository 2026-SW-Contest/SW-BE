package org.swbe.domain.user.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.swbe.domain.user.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

  @Query("""
      select userRole.role.code
      from UserRole userRole
      where userRole.user.id = :userId
        and userRole.revokedAt is null
      """)
  List<String> findActiveRoleCodesByUserId(@Param("userId") Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select userRole
      from UserRole userRole
      where userRole.user.id = :userId
        and userRole.revokedAt is null
      """)
  List<UserRole> findActiveByUserIdForUpdate(@Param("userId") Long userId);
}
