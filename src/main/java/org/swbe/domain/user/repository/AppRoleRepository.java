package org.swbe.domain.user.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.user.entity.AppRole;

public interface AppRoleRepository extends JpaRepository<AppRole, Long> {

  Optional<AppRole> findByCode(String code);
}
