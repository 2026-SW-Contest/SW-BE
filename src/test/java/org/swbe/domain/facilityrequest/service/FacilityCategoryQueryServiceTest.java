package org.swbe.domain.facilityrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.swbe.domain.facilityrequest.entity.FacilityCategory;
import org.swbe.domain.facilityrequest.repository.FacilityCategoryRepository;

@ExtendWith(MockitoExtension.class)
class FacilityCategoryQueryServiceTest {

  @Mock
  private FacilityCategoryRepository facilityCategoryRepository;

  @InjectMocks
  private FacilityCategoryQueryService facilityCategoryQueryService;

  @Test
  void activeCategoriesAreReturnedInIdOrder() {
    FacilityCategory electrical = category(1L, "전기/조명");
    FacilityCategory temperature = category(2L, "냉난방/온도");
    FacilityCategory other = category(8L, "기타");
    when(facilityCategoryRepository.findAllByActiveTrueOrderByIdAsc())
        .thenReturn(List.of(electrical, temperature, other));

    var response = facilityCategoryQueryService.getCategories();

    assertThat(response.data())
        .extracting(category -> category.categoryId())
        .containsExactly(1L, 2L, 8L);
    assertThat(response.data())
        .extracting(category -> category.categoryName())
        .containsExactly("전기/조명", "냉난방/온도", "기타");
  }

  @Test
  void noCategoriesReturnsEmptyList() {
    when(facilityCategoryRepository.findAllByActiveTrueOrderByIdAsc())
        .thenReturn(List.of());

    var response = facilityCategoryQueryService.getCategories();

    assertThat(response.data()).isEmpty();
  }

  private FacilityCategory category(Long id, String name) {
    FacilityCategory category = mock(FacilityCategory.class);
    when(category.getId()).thenReturn(id);
    when(category.getName()).thenReturn(name);
    return category;
  }
}
