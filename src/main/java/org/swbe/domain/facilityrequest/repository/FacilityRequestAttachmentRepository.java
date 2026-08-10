package org.swbe.domain.facilityrequest.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.swbe.domain.facilityrequest.entity.FacilityRequestAttachment;

public interface FacilityRequestAttachmentRepository
    extends JpaRepository<FacilityRequestAttachment, Long> {

  @EntityGraph(attributePaths = "file")
  List<FacilityRequestAttachment>
      findAllByFacilityRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(
          Long facilityRequestId
      );

  @Query("""
      SELECT attachment
      FROM FacilityRequestAttachment attachment
      JOIN FETCH attachment.facilityRequest request
      JOIN FETCH attachment.file file
      WHERE request.id IN :facilityRequestIds
        AND file.deletedAt IS NULL
      ORDER BY request.id, attachment.id
      """)
  List<FacilityRequestAttachment> findPublicImagesByFacilityRequestIds(
      @Param("facilityRequestIds") List<Long> facilityRequestIds
  );
}
