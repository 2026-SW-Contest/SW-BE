package org.swbe.domain.lostitem.dto.request;

import java.time.LocalDate;
import org.swbe.domain.lostitem.entity.StoredItemStatus;

public record StoredItemSearchCondition(
    Long categoryId,
    Long locationId,
    StoredItemStatus status,
    LocalDate from,
    LocalDate to,
    String cursor,
    int size
) {
}
