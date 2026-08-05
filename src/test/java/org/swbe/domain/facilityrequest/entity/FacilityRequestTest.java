package org.swbe.domain.facilityrequest.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.user.entity.AppUser;

class FacilityRequestTest {

  @Test
  void createsReceivedPrivateFacilityRequest() {
    FacilityCategory category = mock(FacilityCategory.class);
    Location location = mock(Location.class);
    AppUser requester = mock(AppUser.class);
    LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 16, 0);

    FacilityRequest request = FacilityRequest.create(
        category,
        location,
        requester,
        "FR-20260801-0001",
        "  Flickering light  ",
        "  The hallway light keeps flickering.  ",
        "  LED light  ",
        createdAt
    );

    assertThat(request.getFacilityCategory()).isSameAs(category);
    assertThat(request.getLocation()).isSameAs(location);
    assertThat(request.getRequester()).isSameAs(requester);
    assertThat(request.getTitle()).isEqualTo("Flickering light");
    assertThat(request.getDescription())
        .isEqualTo("The hallway light keeps flickering.");
    assertThat(request.getEquipmentName()).isEqualTo("LED light");
    assertThat(request.getVisibility()).isEqualTo("PRIVATE");
    assertThat(request.getRequestStatus()).isEqualTo("RECEIVED");
    assertThat(request.getCreatedAt()).isEqualTo(createdAt);
    assertThat(request.getUpdatedAt()).isEqualTo(createdAt);
  }

  @Test
  void requesterIsIdentifiedByUserId() {
    AppUser requester = mock(AppUser.class);
    when(requester.getId()).thenReturn(7L);
    FacilityRequest request = new FacilityRequest();
    ReflectionTestUtils.setField(request, "requester", requester);

    assertThat(request.isRequestedBy(7L)).isTrue();
    assertThat(request.isRequestedBy(8L)).isFalse();
    assertThat(request.isRequestedBy(null)).isFalse();
  }
}
