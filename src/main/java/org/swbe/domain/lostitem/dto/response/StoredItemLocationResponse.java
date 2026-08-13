package org.swbe.domain.lostitem.dto.response;

public record StoredItemLocationResponse(
    Long locationId,
    String name,
    String locationText
) {
}
