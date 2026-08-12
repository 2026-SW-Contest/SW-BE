package org.swbe.domain.lostitem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ItemClaimCreateRequest(
    @NotBlank(message = "ownershipDescription is required")
    @Size(max = 500, message =
        "ownershipDescription must not exceed 500 characters")
    String ownershipDescription
) {

  public ItemClaimCreateRequest {
    ownershipDescription = ownershipDescription == null
        ? null
        : ownershipDescription.strip();
  }
}
