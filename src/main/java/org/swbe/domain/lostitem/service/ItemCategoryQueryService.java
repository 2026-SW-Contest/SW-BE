package org.swbe.domain.lostitem.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.lostitem.dto.response.ItemCategoryListResponse;
import org.swbe.domain.lostitem.dto.response.ItemCategoryResponse;
import org.swbe.domain.lostitem.entity.ItemCategory;
import org.swbe.domain.lostitem.repository.ItemCategoryRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemCategoryQueryService {

  private final ItemCategoryRepository itemCategoryRepository;

  // 등록된 분실물 카테고리를 ID 순서대로 조회하여 반환한다.
  public ItemCategoryListResponse getCategories() {
    List<ItemCategoryResponse> categories = itemCategoryRepository
        .findAllByOrderByIdAsc()
        .stream()
        .map(this::toResponse)
        .toList();

    return new ItemCategoryListResponse(categories);
  }

  // 분실물 카테고리 엔터티를 API 응답 형식으로 변환한다.
  private ItemCategoryResponse toResponse(ItemCategory category) {
    return new ItemCategoryResponse(
        category.getId(),
        category.getName()
    );
  }
}
