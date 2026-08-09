package org.swbe.domain.file.storage;

public record StoredFile(
    String storageProvider,
    String storageKey,
    String originalFilename,
    String mimeType,
    long size,
    String checksum
) {
}
