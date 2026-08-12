package org.swbe.domain.lostitem.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.lostitem.entity.LostItemOffice;

public interface LostItemOfficeRepository
    extends JpaRepository<LostItemOffice, Long> {

  Optional<LostItemOffice> findByIdAndActiveTrue(Long id);
}
