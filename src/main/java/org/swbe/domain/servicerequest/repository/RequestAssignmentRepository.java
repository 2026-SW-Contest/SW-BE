package org.swbe.domain.servicerequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.servicerequest.entity.RequestAssignment;

public interface RequestAssignmentRepository
    extends JpaRepository<RequestAssignment, Long> {

  boolean existsByServiceRequest_IdAndAssignedUser_IdAndEndedAtIsNull(
      Long serviceRequestId,
      Long assignedUserId
  );
}
