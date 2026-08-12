package org.swbe.domain.lostitem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.lostitem.entity.ClaimStatusHistory;

public interface ClaimStatusHistoryRepository
    extends JpaRepository<ClaimStatusHistory, Long> {
}
