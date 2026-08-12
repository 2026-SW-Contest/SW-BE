package org.swbe.domain.lostitem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.lostitem.entity.OfficeStaffAssignment;

public interface OfficeStaffAssignmentRepository
    extends JpaRepository<OfficeStaffAssignment, Long> {

  boolean existsByOffice_IdAndUser_IdAndEndedAtIsNull(
      Long officeId,
      Long userId
  );
}
