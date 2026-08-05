package org.swbe.domain.facilityrequest.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.swbe.domain.user.entity.AppUser;

class FacilityRequestTest {

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
