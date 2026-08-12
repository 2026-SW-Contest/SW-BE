package org.swbe.domain.file.dto;

import org.springframework.core.io.Resource;

public record FileDownload(
    Resource resource,
    String originalFilename,
    String mimeType,
    long size
) {
}
