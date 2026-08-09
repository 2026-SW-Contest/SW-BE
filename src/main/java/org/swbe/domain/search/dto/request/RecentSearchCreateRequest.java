package org.swbe.domain.search.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecentSearchCreateRequest(
    @NotBlank
    @Size(max = 100)
    String keyword
) {
}
