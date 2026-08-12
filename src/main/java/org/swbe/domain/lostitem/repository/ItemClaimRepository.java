package org.swbe.domain.lostitem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.lostitem.entity.ItemClaim;

public interface ItemClaimRepository
    extends JpaRepository<ItemClaim, Long> {

  boolean existsByStoredItem_Id(Long storedItemId);
}
