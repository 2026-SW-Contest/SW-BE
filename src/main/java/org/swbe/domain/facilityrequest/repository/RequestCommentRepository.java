package org.swbe.domain.facilityrequest.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.facilityrequest.entity.RequestComment;

public interface RequestCommentRepository
    extends JpaRepository<RequestComment, Long> {

  List<RequestComment>
      findAllByFacilityRequest_IdAndCommentTypeAndInternalFalseOrderByCreatedAtAscIdAsc(
          Long facilityRequestId,
          String commentType
      );
}
