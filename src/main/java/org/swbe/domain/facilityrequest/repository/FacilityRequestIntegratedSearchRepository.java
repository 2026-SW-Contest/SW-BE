package org.swbe.domain.facilityrequest.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;

public interface FacilityRequestIntegratedSearchRepository {

  long countIntegratedSearchMatches(String pattern);

  List<FacilityRequest> searchIntegratedByCursor(
      String pattern,
      LocalDateTime cursorCreatedAt,
      Long cursorId,
      Pageable pageable
  );
}
