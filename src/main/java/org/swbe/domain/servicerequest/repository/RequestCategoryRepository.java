package org.swbe.domain.servicerequest.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.servicerequest.entity.RequestCategory;

public interface RequestCategoryRepository
    extends JpaRepository<RequestCategory, Long> {

  List<RequestCategory> findAllByActiveTrueOrderByIdAsc();
}
