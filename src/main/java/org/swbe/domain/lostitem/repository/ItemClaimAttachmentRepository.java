package org.swbe.domain.lostitem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.lostitem.entity.ItemClaimAttachment;

public interface ItemClaimAttachmentRepository
    extends JpaRepository<ItemClaimAttachment, Long> {
}
