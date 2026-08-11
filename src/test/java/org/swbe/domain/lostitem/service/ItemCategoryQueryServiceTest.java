package org.swbe.domain.lostitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.swbe.domain.lostitem.dto.response.ItemCategoryListResponse;
import org.swbe.domain.lostitem.entity.ItemCategory;
import org.swbe.domain.lostitem.repository.ItemCategoryRepository;

@ExtendWith(MockitoExtension.class)
class ItemCategoryQueryServiceTest {

  @Mock
  private ItemCategoryRepository itemCategoryRepository;

  @InjectMocks
  private ItemCategoryQueryService itemCategoryQueryService;

  @Test
  void categoriesAreReturnedInIdOrder() {
    ItemCategory electronics = category(1L, "전자기기");
    ItemCategory accessory = category(6L, "액세서리");
    ItemCategory other = category(7L, "기타");
    when(itemCategoryRepository.findAllByOrderByIdAsc())
        .thenReturn(List.of(electronics, accessory, other));

    ItemCategoryListResponse response =
        itemCategoryQueryService.getCategories();

    assertThat(response.data())
        .extracting(category -> category.categoryId())
        .containsExactly(1L, 6L, 7L);
    assertThat(response.data())
        .extracting(category -> category.categoryName())
        .containsExactly("전자기기", "액세서리", "기타");
  }

  @Test
  void noCategoriesReturnsEmptyList() {
    when(itemCategoryRepository.findAllByOrderByIdAsc())
        .thenReturn(List.of());

    ItemCategoryListResponse response =
        itemCategoryQueryService.getCategories();

    assertThat(response.data()).isEmpty();
  }

  // 테스트에 사용할 분실물 카테고리 엔터티를 생성한다.
  private ItemCategory category(Long id, String name) {
    ItemCategory category = mock(ItemCategory.class);
    when(category.getId()).thenReturn(id);
    when(category.getName()).thenReturn(name);
    return category;
  }
}
