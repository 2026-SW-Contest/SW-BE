package org.swbe.domain.lostitem.service;

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
import org.swbe.domain.campus.entity.Department;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.lostitem.entity.LostItemOffice;
import org.swbe.domain.lostitem.repository.LostItemOfficeRepository;

@ExtendWith(MockitoExtension.class)
class LostItemOfficeQueryServiceTest {

  @Mock
  private LostItemOfficeRepository officeRepository;

  @InjectMocks
  private LostItemOfficeQueryService officeQueryService;

  @Test
  void activeOfficesAreReturnedInBuildingAndPrimaryOrder() {
    LostItemOffice s10Office = office(
        4L,
        "MCC 경비실",
        10L,
        "S10",
        "MCC관",
        31L,
        "MCC 경비실",
        "1층",
        "인문학생지원팀",
        true
    );
    LostItemOffice s1AcademicOffice = office(
        2L,
        "인문대학 교학팀",
        1L,
        "S1",
        "본관(종합관)",
        22L,
        "인문대학 교학팀",
        "7층",
        "인문대학 교학팀",
        false
    );
    LostItemOffice s1PrimaryOffice = office(
        1L,
        "인문학생지원팀 분실물 보관소",
        1L,
        "S1",
        "본관(종합관)",
        12L,
        "인문학생지원팀",
        "2층",
        "인문학생지원팀",
        true
    );
    when(officeRepository
        .findAllByActiveTrueAndBuilding_ActiveTrueAndLocation_ActiveTrue())
        .thenReturn(List.of(
            s10Office,
            s1AcademicOffice,
            s1PrimaryOffice
        ));

    var response = officeQueryService.getOffices();

    assertThat(response.data())
        .extracting(office -> office.officeId())
        .containsExactly(1L, 2L, 4L);
    assertThat(response.data().getFirst().buildingCode()).isEqualTo("S1");
    assertThat(response.data().getFirst().locationName())
        .isEqualTo("인문학생지원팀");
    assertThat(response.data().getFirst().departmentName())
        .isEqualTo("인문학생지원팀");
    assertThat(response.data().getFirst().primary()).isTrue();
  }

  @Test
  void noOfficesReturnsEmptyList() {
    when(officeRepository
        .findAllByActiveTrueAndBuilding_ActiveTrueAndLocation_ActiveTrue())
        .thenReturn(List.of());

    var response = officeQueryService.getOffices();

    assertThat(response.data()).isEmpty();
  }

  private LostItemOffice office(
      Long officeId,
      String officeName,
      Long buildingId,
      String buildingCode,
      String buildingName,
      Long locationId,
      String locationName,
      String floor,
      String departmentName,
      boolean primary
  ) {
    Building building = mock(Building.class);
    when(building.getId()).thenReturn(buildingId);
    when(building.getCode()).thenReturn(buildingCode);
    when(building.getName()).thenReturn(buildingName);
    when(building.displayOrder()).thenReturn(buildingId.intValue());

    Location location = mock(Location.class);
    when(location.getId()).thenReturn(locationId);
    when(location.getName()).thenReturn(locationName);
    when(location.getFloor()).thenReturn(floor);

    Department department = mock(Department.class);
    when(department.getName()).thenReturn(departmentName);

    LostItemOffice office = mock(LostItemOffice.class);
    when(office.getId()).thenReturn(officeId);
    when(office.getName()).thenReturn(officeName);
    when(office.getBuilding()).thenReturn(building);
    when(office.getLocation()).thenReturn(location);
    when(office.getDepartment()).thenReturn(department);
    when(office.getOperatingHours()).thenReturn("평일 09:00~17:30");
    when(office.getGuidance()).thenReturn("방문 전 확인");
    when(office.isPrimary()).thenReturn(primary);
    return office;
  }
}
