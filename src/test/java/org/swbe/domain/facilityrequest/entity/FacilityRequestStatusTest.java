package org.swbe.domain.facilityrequest.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FacilityRequestStatusTest {

  @Test
  void exposesOnlySimplifiedStatuses() {
    assertThat(FacilityRequestStatus.values())
        .containsExactly(
            FacilityRequestStatus.WAITING,
            FacilityRequestStatus.IN_PROGRESS,
            FacilityRequestStatus.COMPLETED,
            FacilityRequestStatus.REJECTED,
            FacilityRequestStatus.CANCELED
        );
  }
}
