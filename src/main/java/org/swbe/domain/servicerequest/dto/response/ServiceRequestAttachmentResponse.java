package org.swbe.domain.servicerequest.dto.response;

public record ServiceRequestAttachmentResponse(
    Long fileId,
    String originalFilename,
    String fileUrl
) {
}
