package org.swbe.domain.facilityrequest.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.facilityrequest.dto.response.FacilityCategoryListResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityCategoryResponse;
import org.swbe.domain.facilityrequest.entity.FacilityCategory;
import org.swbe.domain.facilityrequest.repository.FacilityCategoryRepository;

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
