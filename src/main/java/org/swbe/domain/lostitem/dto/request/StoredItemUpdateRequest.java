package org.swbe.domain.lostitem.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StoredItemUpdateRequest {

  @Positive(message = "officeId must be positive")
  private Long officeId;

  @Positive(message = "categoryId must be positive")
  private Long categoryId;

  @Positive(message = "foundLocationId must be positive")
  private Long foundLocationId;

  @Size(max = 255, message =
      "foundLocationText must not exceed 255 characters")
  private String foundLocationText;

  @Size(min = 1, max = 150, message =
      "itemName must be between 1 and 150 characters")
  private String itemName;

  @Size(min = 1, max = 500, message =
      "description must be between 1 and 500 characters")
  private String description;

  @Size(max = 2000, message =
      "privateDescription must not exceed 2000 characters")
  private String privateDescription;

  private boolean privateDescriptionProvided;

  private LocalDate foundDate;

  private List<@Positive(message = "fileId must be positive") Long>
      keepFileIds;

  @JsonSetter
  public void setOfficeId(Long officeId) {
    this.officeId = officeId;
  }

  @JsonSetter
  public void setCategoryId(Long categoryId) {
    this.categoryId = categoryId;
  }

  @JsonSetter
  public void setFoundLocationId(Long foundLocationId) {
    this.foundLocationId = foundLocationId;
  }

  @JsonSetter
  public void setFoundLocationText(String foundLocationText) {
    this.foundLocationText = stripNullable(foundLocationText);
  }

  @JsonSetter
  public void setItemName(String itemName) {
    this.itemName = stripPreservingEmpty(itemName);
  }

  @JsonSetter
  public void setDescription(String description) {
    this.description = stripPreservingEmpty(description);
  }

  @JsonSetter
  public void setPrivateDescription(String privateDescription) {
    this.privateDescriptionProvided = true;
    this.privateDescription = stripNullable(privateDescription);
  }

  @JsonSetter
  public void setFoundDate(LocalDate foundDate) {
    this.foundDate = foundDate;
  }

  @JsonSetter
  public void setKeepFileIds(List<Long> keepFileIds) {
    this.keepFileIds = keepFileIds == null
        ? null
        : List.copyOf(keepFileIds);
  }

  public boolean hasFoundLocationChange() {
    return foundLocationId != null || foundLocationText != null;
  }

  public boolean hasValidFoundLocationChange() {
    return foundLocationId != null;
  }

  public boolean hasChanges() {
    return officeId != null
        || categoryId != null
        || hasFoundLocationChange()
        || itemName != null
        || description != null
        || privateDescriptionProvided
        || foundDate != null
        || keepFileIds != null;
  }

  private static String stripNullable(String value) {
    if (value == null) {
      return null;
    }
    String stripped = value.strip();
    return stripped.isEmpty() ? null : stripped;
  }

  private static String stripPreservingEmpty(String value) {
    return value == null ? null : value.strip();
  }
}
