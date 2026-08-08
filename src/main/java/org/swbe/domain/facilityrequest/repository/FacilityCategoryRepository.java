package org.swbe.domain.facilityrequest.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.facilityrequest.entity.FacilityCategory;

public interface FacilityCategoryRepository
    extends JpaRepository<FacilityCategory, Long> {

  List<FacilityCategory> findAllByActiveTrueOrderByIdAsc();

  Optional<FacilityCategory> findByIdAndActiveTrue(Long id);
}
