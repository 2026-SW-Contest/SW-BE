package org.swbe.domain.campus.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BuildingTest {

  @Test
  void serviceBuildingCodeReturnsItsNumericDisplayOrder() {
    Building building = buildingWithCode("S10");

    assertThat(building.displayOrder()).isEqualTo(10);
  }

  @Test
  void missingOrNonServiceBuildingCodeIsDisplayedLast() {
    Building missingCode = buildingWithCode(null);
    Building nonServiceCode = buildingWithCode("OTHER");

    assertThat(missingCode.displayOrder()).isEqualTo(Integer.MAX_VALUE);
    assertThat(nonServiceCode.displayOrder()).isEqualTo(Integer.MAX_VALUE);
  }

  private Building buildingWithCode(String code) {
    Building building = new Building();
    ReflectionTestUtils.setField(building, "code", code);
    return building;
  }
}
