package org.swbe.domain.servicerequest.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.servicerequest.entity.ServiceRequestAttachment;

public interface ServiceRequestAttachmentRepository
    extends JpaRepository<ServiceRequestAttachment, Long> {

  @EntityGraph(attributePaths = "file")
  List<ServiceRequestAttachment>
      findAllByServiceRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(
          Long serviceRequestId
      );
}
