package org.swbe.domain.lostitem.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.swbe.domain.lostitem.entity.ItemClaimAttachment;

public interface ItemClaimAttachmentRepository
    extends JpaRepository<ItemClaimAttachment, Long> {

  @Query("""
      SELECT attachment
      FROM ItemClaimAttachment attachment
      JOIN FETCH attachment.itemClaim claim
      JOIN FETCH attachment.file file
      WHERE claim.id IN :itemClaimIds
        AND file.deletedAt IS NULL
      ORDER BY claim.id, attachment.id
      """)
  List<ItemClaimAttachment> findPublicImagesByItemClaimIds(
      @Param("itemClaimIds") List<Long> itemClaimIds
  );

  @Query("""
      SELECT attachment
      FROM ItemClaimAttachment attachment
      JOIN FETCH attachment.file file
      WHERE attachment.itemClaim.id = :itemClaimId
        AND file.deletedAt IS NULL
      ORDER BY attachment.id
      """)
  List<ItemClaimAttachment> findPublicImagesByItemClaimId(
      @Param("itemClaimId") Long itemClaimId
  );
}
