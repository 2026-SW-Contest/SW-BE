package org.swbe.domain.campus.service;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.campus.dto.response.LocationListResponse;
import org.swbe.domain.campus.dto.response.LocationResponse;
import org.swbe.domain.campus.entity.Building;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.campus.repository.LocationRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationQueryService {

  private final LocationRepository locationRepository;

  public LocationListResponse getLocations() {
    List<LocationResponse> locations = locationRepository
        .findAllByActiveTrueAndBuilding_ActiveTrue()
        .stream()
        .sorted(Comparator.comparingInt(
            location -> location.getBuilding().displayOrder()
        ))
        .map(this::toResponse)
        .toList();

    return new LocationListResponse(locations);
  }

  private LocationResponse toResponse(Location location) {
    Building building = location.getBuilding();
    return new LocationResponse(
        location.getId(),
        building.getCode(),
        location.getName()
    );
  }
}
