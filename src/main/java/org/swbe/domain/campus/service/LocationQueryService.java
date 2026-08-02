package org.swbe.domain.campus.service;

import java.util.Comparator;
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

  private static final int OTHER_LOCATION_ORDER = Integer.MAX_VALUE;

  private final LocationRepository locationRepository;

  public LocationListResponse getLocations() {
    var locations = locationRepository
        .findAllByActiveTrueAndBuilding_ActiveTrue()
        .stream()
        .sorted(Comparator.comparingInt(this::locationOrder))
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

  private int locationOrder(Location location) {
    String code = location.getBuilding().getCode();
    if (code == null || !code.matches("S\\d+")) {
      return OTHER_LOCATION_ORDER;
    }

    return Integer.parseInt(code.substring(1));
  }
}
