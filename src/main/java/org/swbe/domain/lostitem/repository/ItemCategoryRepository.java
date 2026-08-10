package org.swbe.domain.lostitem.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.lostitem.entity.ItemCategory;

public interface ItemCategoryRepository
    extends JpaRepository<ItemCategory, Long> {

  // 분실물 카테고리를 ID 오름차순으로 조회한다.
  List<ItemCategory> findAllByOrderByIdAsc();
}
