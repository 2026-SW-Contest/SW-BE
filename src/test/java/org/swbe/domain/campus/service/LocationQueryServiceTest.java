package org.swbe.domain.campus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.swbe.domain.campus.entity.Building;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.campus.repository.LocationRepository;

@ExtendWith(MockitoExtension.class)
class LocationQueryServiceTest {

  @Mock
  private LocationRepository locationRepository;

  @InjectMocks
  private LocationQueryService locationQueryService;

  @Test
  void activeRootLocationsAreReturnedInCampusOrder() {
    Location other = location(11L, null, "기타");
    Location s10 = location(10L, "S10", "MCC관");
    Location s2 = location(2L, "S2", "학생회관");
    Location s1 = location(1L, "S1", "본관(종합관)");
    when(locationRepository
        .findAllByActiveTrueAndBuilding_ActiveTrueAndParentIsNull())
        .thenReturn(List.of(other, s10, s2, s1));

    var response = locationQueryService.getLocations();

    assertThat(response.data())
        .extracting(location -> location.locationId())
        .containsExactly(1L, 2L, 10L, 11L);
    assertThat(response.data())
        .extracting(location -> location.locationCode())
        .containsExactly("S1", "S2", "S10", null);
    assertThat(response.data())
        .extracting(location -> location.locationName())
        .containsExactly("본관(종합관)", "학생회관", "MCC관", "기타");
  }

  @Test
  void noLocationsReturnsEmptyList() {
    when(locationRepository
        .findAllByActiveTrueAndBuilding_ActiveTrueAndParentIsNull())
        .thenReturn(List.of());

    var response = locationQueryService.getLocations();

    assertThat(response.data()).isEmpty();
  }

  private Location location(Long id, String code, String name) {
    Building building = mock(Building.class);
    when(building.getCode()).thenReturn(code);
    when(building.displayOrder()).thenReturn(
        code == null ? Integer.MAX_VALUE : id.intValue()
    );

    Location location = mock(Location.class);
    when(location.getId()).thenReturn(id);
    when(location.getName()).thenReturn(name);
    when(location.getBuilding()).thenReturn(building);
    return location;
  }
}
