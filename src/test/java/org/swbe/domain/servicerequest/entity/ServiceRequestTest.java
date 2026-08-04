package org.swbe.domain.servicerequest.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.swbe.domain.user.entity.AppUser;

class ServiceRequestTest {

  @Test
  void requesterIsIdentifiedByUserId() {
    AppUser requester = mock(AppUser.class);
    when(requester.getId()).thenReturn(7L);
    ServiceRequest request = new ServiceRequest();
    ReflectionTestUtils.setField(request, "requester", requester);

    assertThat(request.isRequestedBy(7L)).isTrue();
    assertThat(request.isRequestedBy(8L)).isFalse();
    assertThat(request.isRequestedBy(null)).isFalse();
  }
}
