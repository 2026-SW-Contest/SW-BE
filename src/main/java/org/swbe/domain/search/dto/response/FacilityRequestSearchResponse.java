package org.swbe.domain.search.dto.response;

public record FacilityRequestSearchResponse(
    CursorSliceResponse<FacilityRequestSearchItemResponse> data
) {
}
