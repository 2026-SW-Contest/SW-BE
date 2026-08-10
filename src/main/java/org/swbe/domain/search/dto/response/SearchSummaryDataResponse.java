package org.swbe.domain.search.dto.response;

public record SearchSummaryDataResponse(
    String keyword,
    long lostItemCount,
    long facilityRequestCount
) {
}
