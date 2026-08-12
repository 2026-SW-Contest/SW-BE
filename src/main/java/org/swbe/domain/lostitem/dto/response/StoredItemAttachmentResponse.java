package org.swbe.domain.lostitem.dto.response;

public record StoredItemAttachmentResponse(
    Long fileId,
    String originalFilename,
    String fileUrl
) {
}
