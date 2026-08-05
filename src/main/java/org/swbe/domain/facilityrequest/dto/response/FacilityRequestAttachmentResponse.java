package org.swbe.domain.facilityrequest.dto.response;

public record FacilityRequestAttachmentResponse(
    Long fileId,
    String originalFilename,
    String fileUrl
) {
}
