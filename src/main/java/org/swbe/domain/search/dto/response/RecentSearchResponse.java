package org.swbe.domain.search.dto.response;

import java.time.LocalDateTime;

public record RecentSearchResponse(
    Long recentSearchId,
    String keyword,
    LocalDateTime searchedAt
) {
}
