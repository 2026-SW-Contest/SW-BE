package org.swbe.domain.lostitem.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.swbe.domain.lostitem.entity.StoredItemStatus;

public record StoredItemStatusUpdateRequest(
    @NotNull(message = "status is required")
    StoredItemStatus status,

    @Size(max = 1000, message =
        "changeReason must not exceed 1000 characters")
    String changeReason
) {

  public StoredItemStatusUpdateRequest {
    changeReason = stripNullable(changeReason);
  }

  private static String stripNullable(String value) {
    if (value == null) {
      return null;
    }
    String stripped = value.strip();
    return stripped.isEmpty() ? null : stripped;
  }
}
