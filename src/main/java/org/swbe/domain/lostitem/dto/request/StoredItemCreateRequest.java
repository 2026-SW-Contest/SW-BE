package org.swbe.domain.lostitem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record StoredItemCreateRequest(
    @NotNull(message = "officeId is required")
    @Positive(message = "officeId must be positive")
    Long officeId,

    @NotNull(message = "categoryId is required")
    @Positive(message = "categoryId must be positive")
    Long categoryId,

    @NotNull(message = "foundLocationId is required")
    @Positive(message = "foundLocationId must be positive")
    Long foundLocationId,

    @Size(max = 255, message =
        "foundLocationText must not exceed 255 characters")
    String foundLocationText,

    @NotBlank(message = "itemName is required")
    @Size(max = 150, message =
        "itemName must not exceed 150 characters")
    String itemName,

    @NotBlank(message = "description is required")
    @Size(max = 500, message =
        "description must not exceed 500 characters")
    String description,

    @Size(max = 2000, message =
        "privateDescription must not exceed 2000 characters")
    String privateDescription,

    @NotNull(message = "foundDate is required")
    LocalDate foundDate
) {

  public StoredItemCreateRequest {
    foundLocationText = stripNullable(foundLocationText);
    itemName = stripNullable(itemName);
    description = stripNullable(description);
    privateDescription = stripNullable(privateDescription);
  }

  public boolean hasRequiredFoundLocation() {
    return foundLocationId != null;
  }

  private static String stripNullable(String value) {
    if (value == null) {
      return null;
    }
    String stripped = value.strip();
    return stripped.isEmpty() ? null : stripped;
  }
}
