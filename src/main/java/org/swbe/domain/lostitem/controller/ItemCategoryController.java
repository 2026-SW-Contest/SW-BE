package org.swbe.domain.lostitem.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.lostitem.dto.response.ItemCategoryListResponse;
import org.swbe.domain.lostitem.service.ItemCategoryQueryService;

@RestController
@RequestMapping("/api/item-categories")
@RequiredArgsConstructor
public class ItemCategoryController {

  private final ItemCategoryQueryService itemCategoryQueryService;

  // 분실물 등록과 필터에서 사용할 카테고리 목록을 조회한다.
  @GetMapping
  public ItemCategoryListResponse getCategories() {
    return itemCategoryQueryService.getCategories();
  }
}
