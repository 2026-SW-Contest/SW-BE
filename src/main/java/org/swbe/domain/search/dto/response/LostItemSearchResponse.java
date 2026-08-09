package org.swbe.domain.search.dto.response;

public record LostItemSearchResponse(
    CursorSliceResponse<LostItemSearchItemResponse> data
) {
}
