package org.swbe.domain.lostitem.repository;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  Optional<ItemClaim>
      findFirstByStoredItem_IdAndClaimantUser_IdOrderByCreatedAtDescIdDesc(
          Long storedItemId,
          Long claimantUserId
      );

  @Query("""
      SELECT claim
      FROM ItemClaim claim
      JOIN FETCH claim.storedItem item
      LEFT JOIN FETCH claim.claimantUser
      LEFT JOIN FETCH claim.temporaryClaimant
      WHERE item.id = :storedItemId
        AND (:status IS NULL OR claim.claimStatus = :status)
        AND (
          :cursorCreatedAt IS NULL
          OR claim.createdAt < :cursorCreatedAt
          OR (
            claim.createdAt = :cursorCreatedAt
            AND claim.id < :cursorId
          )
        )
      ORDER BY claim.createdAt DESC, claim.id DESC
      """)
  List<ItemClaim> findAllByCursor(
      @Param("storedItemId") Long storedItemId,
      @Param("status") ItemClaimStatus status,
      @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
      @Param("cursorId") Long cursorId,
      Pageable pageable
  );

  @Query("""
      SELECT claim
      FROM ItemClaim claim
      JOIN FETCH claim.storedItem item
      LEFT JOIN FETCH claim.claimantUser
      LEFT JOIN FETCH claim.temporaryClaimant
      WHERE item.office.id = :officeId
        AND (:status IS NULL OR claim.claimStatus = :status)
        AND (
          :cursorCreatedAt IS NULL
          OR claim.createdAt < :cursorCreatedAt
          OR (
            claim.createdAt = :cursorCreatedAt
            AND claim.id < :cursorId
          )
        )
      ORDER BY claim.createdAt DESC, claim.id DESC
      """)
  List<ItemClaim> findAllByOfficeIdAndCursor(
      @Param("officeId") Long officeId,
      @Param("status") ItemClaimStatus status,
      @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
      @Param("cursorId") Long cursorId,
      Pageable pageable
  );

  @Query("""
      SELECT claim
      FROM ItemClaim claim
      JOIN FETCH claim.storedItem item
      JOIN FETCH item.office
      LEFT JOIN FETCH claim.claimantUser
      LEFT JOIN FETCH claim.temporaryClaimant
      WHERE claim.id = :itemClaimId
      """)
  Optional<ItemClaim> findDetailById(
      @Param("itemClaimId") Long itemClaimId
  );

  @Query("""
      SELECT claim
      FROM ItemClaim claim
      LEFT JOIN FETCH claim.claimantUser
      LEFT JOIN FETCH claim.temporaryClaimant
      WHERE claim.storedItem.id = :storedItemId
        AND claim.claimStatus = :status
      ORDER BY claim.id
      """)
  List<ItemClaim> findAllByStoredItemIdAndStatus(
      @Param("storedItemId") Long storedItemId,
      @Param("status") ItemClaimStatus status
  );

  @Query("""
      SELECT claim
      FROM ItemClaim claim
      JOIN FETCH claim.storedItem item
      JOIN FETCH item.itemCategory category
      LEFT JOIN FETCH item.foundLocation location
      WHERE claim.claimantUser.id = :claimantUserId
        AND (
          :cursorCreatedAt IS NULL
          OR claim.createdAt < :cursorCreatedAt
          OR (
            claim.createdAt = :cursorCreatedAt
            AND claim.id < :cursorId
          )
        )
      ORDER BY claim.createdAt DESC, claim.id DESC
      """)
  List<ItemClaim> findAllByClaimantUserIdAndCursor(
      @Param("claimantUserId") Long claimantUserId,
      @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
      @Param("cursorId") Long cursorId,
      Pageable pageable
  );
}
