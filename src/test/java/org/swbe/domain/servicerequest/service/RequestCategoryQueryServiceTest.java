package org.swbe.domain.servicerequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.swbe.domain.servicerequest.entity.RequestCategory;
import org.swbe.domain.servicerequest.repository.RequestCategoryRepository;

@ExtendWith(MockitoExtension.class)
class RequestCategoryQueryServiceTest {

  @Mock
  private RequestCategoryRepository requestCategoryRepository;

  @InjectMocks
  private RequestCategoryQueryService requestCategoryQueryService;

  @Test
  void activeCategoriesAreReturnedInIdOrder() {
    RequestCategory electrical = category(1L, "전기/조명");
    RequestCategory temperature = category(2L, "냉난방/온도");
    RequestCategory other = category(8L, "기타");
    when(requestCategoryRepository.findAllByActiveTrueOrderByIdAsc())
        .thenReturn(List.of(electrical, temperature, other));

    var response = requestCategoryQueryService.getCategories();

    assertThat(response.data())
        .extracting(category -> category.categoryId())
        .containsExactly(1L, 2L, 8L);
    assertThat(response.data())
        .extracting(category -> category.categoryName())
        .containsExactly("전기/조명", "냉난방/온도", "기타");
  }

  @Test
  void noCategoriesReturnsEmptyList() {
    when(requestCategoryRepository.findAllByActiveTrueOrderByIdAsc())
        .thenReturn(List.of());

    var response = requestCategoryQueryService.getCategories();

    assertThat(response.data()).isEmpty();
  }

  private RequestCategory category(Long id, String name) {
    RequestCategory category = mock(RequestCategory.class);
    when(category.getId()).thenReturn(id);
    when(category.getName()).thenReturn(name);
    return category;
  }
}
