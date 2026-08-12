package org.swbe.domain.facilityrequest.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;

public record AdminFacilityRequestProcessRequest(
    FacilityRequestStatus status,
    @Size(max = 2000)
    @Pattern(regexp = "(?s).*\\S.*")
    String adminResponse
) {

  public AdminFacilityRequestProcessRequest {
    adminResponse = stripNullable(adminResponse);
  }

  public boolean hasAdminResponse() {
    return adminResponse != null;
  }

  private static String stripNullable(String value) {
    return value == null ? null : value.strip();
  }
}
