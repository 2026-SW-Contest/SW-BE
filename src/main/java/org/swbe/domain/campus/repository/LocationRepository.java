package org.swbe.domain.campus.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.campus.entity.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {

  @EntityGraph(attributePaths = "building")
  List<Location> findAllByActiveTrueAndBuilding_ActiveTrue();
}
