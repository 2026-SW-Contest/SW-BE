package org.swbe.domain.facilityrequest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.swbe.domain.facilityrequest.dto.request.AdminFacilityRequestSearchCondition;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;

public interface AdminFacilityRequestSearchRepository {

  Page<FacilityRequest> searchAdminRequests(
      AdminFacilityRequestSearchCondition condition,
      Pageable pageable
  );
}
