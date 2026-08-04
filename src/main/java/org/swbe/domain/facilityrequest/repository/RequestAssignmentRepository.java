package org.swbe.domain.facilityrequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.facilityrequest.entity.RequestAssignment;

public interface RequestAssignmentRepository
    extends JpaRepository<RequestAssignment, Long> {

  boolean existsByFacilityRequest_IdAndAssignedUser_IdAndEndedAtIsNull(
      Long facilityRequestId,
      Long assignedUserId
  );
}
