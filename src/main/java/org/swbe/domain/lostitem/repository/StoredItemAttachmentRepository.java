package org.swbe.domain.lostitem.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.swbe.domain.lostitem.entity.StoredItemAttachment;

public interface StoredItemAttachmentRepository
    extends JpaRepository<StoredItemAttachment, Long> {

  @Query("""
      SELECT attachment
      FROM StoredItemAttachment attachment
      JOIN FETCH attachment.storedItem item
      JOIN FETCH attachment.file file
      WHERE item.id IN :storedItemIds
        AND file.deletedAt IS NULL
      ORDER BY
        item.id,
        CASE WHEN attachment.primary = true THEN 0 ELSE 1 END,
        attachment.displayOrder,
        attachment.id
      """)
  List<StoredItemAttachment> findPublicImagesByStoredItemIds(
      @Param("storedItemIds") List<Long> storedItemIds
  );
}
