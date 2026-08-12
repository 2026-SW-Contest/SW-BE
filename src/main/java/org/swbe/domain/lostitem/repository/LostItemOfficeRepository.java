package org.swbe.domain.lostitem.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.lostitem.entity.LostItemOffice;

public interface LostItemOfficeRepository
    extends JpaRepository<LostItemOffice, Long> {

  Optional<LostItemOffice> findByIdAndActiveTrue(Long id);

  @EntityGraph(attributePaths = {
      "building",
      "location",
      "department"
  })
  List<LostItemOffice>
      findAllByActiveTrueAndBuilding_ActiveTrueAndLocation_ActiveTrue();
}
