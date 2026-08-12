package org.swbe.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.swbe.domain.file.dto.FileDownload;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.exception.FileErrorCode;
import org.swbe.domain.file.repository.FileResourceRepository;
import org.swbe.domain.file.storage.FileStorageException;
import org.swbe.domain.file.storage.FileStorageRegistry;
import org.swbe.domain.file.storage.ReadableFileStorage;
import org.swbe.global.error.BusinessException;

class FileQueryServiceTest {

  private FileResourceRepository fileResourceRepository;
  private FileStorageRegistry fileStorageRegistry;
  private ReadableFileStorage localStorage;
  private FileQueryService service;

  @BeforeEach
  void setUp() {
    fileResourceRepository = mock(FileResourceRepository.class);
    fileStorageRegistry = mock(FileStorageRegistry.class);
    localStorage = mock(ReadableFileStorage.class);
    service = new FileQueryService(
        fileResourceRepository,
        fileStorageRegistry
    );
  }

  @Test
  void returnsLocalPublicImage() {
    FileResource file = localFile("image/jpeg");
    ByteArrayResource resource = new ByteArrayResource(
        "image".getBytes(StandardCharsets.UTF_8)
    );
    when(fileResourceRepository
        .findByIdAndStorageProviderAndDeletedAtIsNull(1L, "LOCAL"))
        .thenReturn(Optional.of(file));
    when(fileStorageRegistry.get("LOCAL")).thenReturn(localStorage);
    when(localStorage.load("2026/08/10/image.jpg"))
        .thenReturn(resource);

    FileDownload result = service.getLocalPublicImage(1L);

    assertThat(result.resource()).isSameAs(resource);
    assertThat(result.originalFilename()).isEqualTo("image.jpg");
    assertThat(result.mimeType()).isEqualTo("image/jpeg");
    assertThat(result.size()).isEqualTo(5L);
  }

  @Test
  void rejectsMissingOrNonLocalFile() {
    when(fileResourceRepository
        .findByIdAndStorageProviderAndDeletedAtIsNull(1L, "LOCAL"))
        .thenReturn(Optional.empty());

    assertFileError(
        () -> service.getLocalPublicImage(1L),
        FileErrorCode.NOT_FOUND
    );
  }

  @Test
  void rejectsNonImageFile() {
    FileResource file = localFile("application/pdf");
    when(fileResourceRepository
        .findByIdAndStorageProviderAndDeletedAtIsNull(1L, "LOCAL"))
        .thenReturn(Optional.of(file));

    assertFileError(
        () -> service.getLocalPublicImage(1L),
        FileErrorCode.NOT_FOUND
    );
  }

  @Test
  void convertsLocalStorageFailureToFileError() {
    FileResource file = localFile("image/jpeg");
    when(fileResourceRepository
        .findByIdAndStorageProviderAndDeletedAtIsNull(1L, "LOCAL"))
        .thenReturn(Optional.of(file));
    when(fileStorageRegistry.get("LOCAL")).thenReturn(localStorage);
    when(localStorage.load("2026/08/10/image.jpg"))
        .thenThrow(new FileStorageException(
            "failed",
            new IllegalStateException("cause")
        ));

    assertFileError(
        () -> service.getLocalPublicImage(1L),
        FileErrorCode.STORAGE_ERROR
    );
  }

  private FileResource localFile(String mimeType) {
    FileResource file = mock(FileResource.class);
    when(file.getStorageKey()).thenReturn("2026/08/10/image.jpg");
    when(file.getOriginalFilename()).thenReturn("image.jpg");
    when(file.getMimeType()).thenReturn(mimeType);
    when(file.getFileSize()).thenReturn(5L);
    return file;
  }

  private void assertFileError(
      Runnable action,
      FileErrorCode expectedErrorCode
  ) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(expectedErrorCode)
        );
  }
}
