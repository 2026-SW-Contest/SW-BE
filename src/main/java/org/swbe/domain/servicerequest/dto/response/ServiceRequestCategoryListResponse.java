package org.swbe.domain.servicerequest.dto.response;

import java.util.List;

public record ServiceRequestCategoryListResponse(
    List<ServiceRequestCategoryResponse> data
) {

  public ServiceRequestCategoryListResponse {
    data = List.copyOf(data);
  }
}
