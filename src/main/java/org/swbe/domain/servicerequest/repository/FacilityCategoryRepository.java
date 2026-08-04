package org.swbe.domain.servicerequest.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.servicerequest.entity.FacilityCategory;

public interface FacilityCategoryRepository
    extends JpaRepository<FacilityCategory, Long> {

  List<FacilityCategory> findAllByActiveTrueOrderByIdAsc();
}
