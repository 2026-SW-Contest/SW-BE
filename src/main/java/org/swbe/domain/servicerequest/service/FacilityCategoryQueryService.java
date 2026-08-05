package org.swbe.domain.servicerequest.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.servicerequest.dto.response.FacilityCategoryListResponse;
import org.swbe.domain.servicerequest.dto.response.FacilityCategoryResponse;
import org.swbe.domain.servicerequest.entity.FacilityCategory;
import org.swbe.domain.servicerequest.repository.FacilityCategoryRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityCategoryQueryService {

  private final FacilityCategoryRepository facilityCategoryRepository;

  public FacilityCategoryListResponse getCategories() {
    List<FacilityCategoryResponse> categories = facilityCategoryRepository
        .findAllByActiveTrueOrderByIdAsc()
        .stream()
        .map(this::toResponse)
        .toList();

    return new FacilityCategoryListResponse(categories);
  }

  private FacilityCategoryResponse toResponse(
      FacilityCategory category
  ) {
    return new FacilityCategoryResponse(
        category.getId(),
        category.getName()
    );
  }
}
