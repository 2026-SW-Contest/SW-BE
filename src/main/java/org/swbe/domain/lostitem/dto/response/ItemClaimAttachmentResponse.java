package org.swbe.domain.lostitem.dto.response;

public record ItemClaimAttachmentResponse(
    Long fileId,
    String originalFilename,
    String fileUrl
) {
}
