package org.swbe.domain.lostitem.dto.request;

import org.swbe.domain.lostitem.entity.ItemClaimStatus;

public record ItemClaimSearchCondition(
    ItemClaimStatus status,
    String cursor,
    int size
) {
}
