package org.swbe.domain.lostitem.repository;

import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.lostitem.entity.ItemClaim;
import org.swbe.domain.lostitem.entity.ItemClaimStatus;

public interface ItemClaimRepository
    extends JpaRepository<ItemClaim, Long> {

  boolean existsByStoredItem_Id(Long storedItemId);

  boolean existsByStoredItem_IdAndClaimantUser_IdAndClaimStatusIn(
      Long storedItemId,
      Long claimantUserId,
      Collection<ItemClaimStatus> claimStatuses
  );
}
