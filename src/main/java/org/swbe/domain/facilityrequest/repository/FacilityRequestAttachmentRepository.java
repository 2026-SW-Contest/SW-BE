package org.swbe.domain.facilityrequest.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.facilityrequest.entity.FacilityRequestAttachment;

public interface FacilityRequestAttachmentRepository
    extends JpaRepository<FacilityRequestAttachment, Long> {

  @EntityGraph(attributePaths = "file")
  List<FacilityRequestAttachment>
      findAllByFacilityRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(
          Long facilityRequestId
      );
}
