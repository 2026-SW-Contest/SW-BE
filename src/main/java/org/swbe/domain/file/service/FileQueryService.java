package org.swbe.domain.file.service;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.file.dto.FileDownload;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.exception.FileErrorCode;
import org.swbe.domain.file.repository.FileResourceRepository;
import org.swbe.domain.file.storage.FileStorage;
import org.swbe.domain.file.storage.FileStorageException;
import org.swbe.domain.file.storage.FileStorageRegistry;
import org.swbe.domain.file.storage.ReadableFileStorage;
import org.swbe.global.error.BusinessException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileQueryService {

  private static final String LOCAL_PROVIDER = "LOCAL";
  private static final Set<String> PUBLIC_IMAGE_TYPES = Set.of(
      "image/jpeg",
      "image/png",
      "image/gif",
      "image/webp"
  );

  private final FileResourceRepository fileResourceRepository;
  private final FileStorageRegistry fileStorageRegistry;

  public FileDownload getLocalPublicImage(Long fileId) {
    FileResource file = fileResourceRepository
        .findByIdAndStorageProviderAndDeletedAtIsNull(
            fileId,
            LOCAL_PROVIDER
        )
        .filter(resource ->
            PUBLIC_IMAGE_TYPES.contains(resource.getMimeType())
        )
        .orElseThrow(() -> new BusinessException(
            FileErrorCode.NOT_FOUND
        ));
    FileStorage storage = fileStorageRegistry.get(LOCAL_PROVIDER);
    if (!(storage instanceof ReadableFileStorage readableStorage)) {
      throw new BusinessException(
          FileErrorCode.STORAGE_PROVIDER_NOT_SUPPORTED
      );
    }

    try {
      return new FileDownload(
          readableStorage.load(file.getStorageKey()),
          file.getOriginalFilename(),
          file.getMimeType(),
          file.getFileSize()
      );
    } catch (FileStorageException exception) {
      throw new BusinessException(FileErrorCode.STORAGE_ERROR);
    }
  }
}
