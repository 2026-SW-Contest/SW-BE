package org.swbe.domain.facilityrequest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FacilityRequestCreateRequest(
    @NotNull(message = "categoryId is required")
    @Positive(message = "categoryId must be positive")
    Long categoryId,

    @NotNull(message = "locationId is required")
    @Positive(message = "locationId must be positive")
    Long locationId,

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must not exceed 200 characters")
    String title,

    @NotBlank(message = "description is required")
    @Size(max = 500, message = "description must not exceed 500 characters")
    String description,

    @Size(max = 150, message = "equipmentName must not exceed 150 characters")
    String equipmentName
) {

  public FacilityRequestCreateRequest {
    title = stripNullable(title);
    description = stripNullable(description);
    equipmentName = stripNullable(equipmentName);
  }

  private static String stripNullable(String value) {
    return value == null ? null : value.strip();
  }
}
