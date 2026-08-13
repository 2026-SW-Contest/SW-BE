package org.swbe.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.swbe.domain.file.config.FileStorageProperties;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.exception.FileErrorCode;
import org.swbe.global.error.BusinessException;

class FilePublicUrlResolverTest {

  private final FilePublicUrlResolver resolver = new FilePublicUrlResolver(
      new FileStorageProperties(
          new FileStorageProperties.S3(
              "bucket",
              "ap-northeast-2",
              "https://cdn.example.com/",
              Duration.ofMinutes(10)
          )
      )
  );

  @Test
  void resolvesS3FileToCloudFrontUrl() {
    FileResource file = file(
        1L,
        "S3",
        "2026/08/10/image name.jpg"
    );

    assertThat(resolver.resolve(file)).isEqualTo(
        "https://cdn.example.com/2026/08/10/image%20name.jpg"
    );
  }

  @Test
  void rejectsUnknownStorageProvider() {
    FileResource file = file(1L, "UNKNOWN", "image.jpg");

    assertThatThrownBy(() -> resolver.resolve(file))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(FileErrorCode.STORAGE_PROVIDER_NOT_SUPPORTED)
        );
  }

  @Test
  void rejectsPrivateS3File() {
    FileResource file = file(
        1L,
        "S3_PRIVATE",
        "private/item-claims/2026/08/12/proof.jpg"
    );

    assertThatThrownBy(() -> resolver.resolve(file))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(FileErrorCode.STORAGE_PROVIDER_NOT_SUPPORTED)
        );
  }

  private FileResource file(Long id, String provider, String storageKey) {
    FileResource file = mock(FileResource.class);
    when(file.getId()).thenReturn(id);
    when(file.getStorageProvider()).thenReturn(provider);
    when(file.getStorageKey()).thenReturn(storageKey);
    return file;
  }
}
