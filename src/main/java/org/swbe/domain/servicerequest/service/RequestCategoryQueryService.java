package org.swbe.domain.servicerequest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestCategoryListResponse;
import org.swbe.domain.servicerequest.dto.response.ServiceRequestCategoryResponse;
import org.swbe.domain.servicerequest.entity.RequestCategory;
import org.swbe.domain.servicerequest.repository.RequestCategoryRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestCategoryQueryService {

  private final RequestCategoryRepository requestCategoryRepository;

  public ServiceRequestCategoryListResponse getCategories() {
    var categories = requestCategoryRepository
        .findAllByActiveTrueOrderByIdAsc()
        .stream()
        .map(this::toResponse)
        .toList();

    return new ServiceRequestCategoryListResponse(categories);
  }

  private ServiceRequestCategoryResponse toResponse(
      RequestCategory category
  ) {
    return new ServiceRequestCategoryResponse(
        category.getId(),
        category.getName()
    );
  }
}
