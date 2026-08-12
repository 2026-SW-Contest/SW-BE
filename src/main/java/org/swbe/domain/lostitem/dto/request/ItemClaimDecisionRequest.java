package org.swbe.domain.lostitem.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.swbe.domain.lostitem.entity.ItemClaimStatus;

public record ItemClaimDecisionRequest(
    @NotNull(message = "decision is required")
    ItemClaimStatus decision,

    @Size(max = 1000, message =
        "message must not exceed 1000 characters")
    String message
) {

  public ItemClaimDecisionRequest {
    message = stripNullable(message);
  }

  private static String stripNullable(String value) {
    if (value == null) {
      return null;
    }
    String stripped = value.strip();
    return stripped.isEmpty() ? null : stripped;
  }
}
