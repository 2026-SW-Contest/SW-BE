package org.swbe.domain.lostitem.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.swbe.domain.lostitem.entity.ClaimStatusHistory;

public interface ClaimStatusHistoryRepository
    extends JpaRepository<ClaimStatusHistory, Long> {

  @Query("""
      SELECT history
      FROM ClaimStatusHistory history
      LEFT JOIN FETCH history.changedBy
      WHERE history.itemClaim.id = :itemClaimId
      ORDER BY history.id
      """)
  List<ClaimStatusHistory> findAllByItemClaimId(
      @Param("itemClaimId") Long itemClaimId
  );
}
