package org.swbe.domain.servicerequest.dto.response;

import java.util.List;

public record ServiceRequestPageResponse(
    List<ServiceRequestListItemResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {

  public ServiceRequestPageResponse {
    content = List.copyOf(content);
  }
}
