package org.swbe.domain.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.swbe.domain.file.config.FileStorageProperties;

class FileStorageRegistryTest {

  @Test
  void selectsConfiguredWriteStorageAndProviderCaseInsensitively() {
    FileStorage local = storage("LOCAL");
    FileStorage s3 = storage("S3");
    FileStorage privateS3 = storage("S3_PRIVATE");
    FileStorageRegistry registry = new FileStorageRegistry(
        List.of(local, s3, privateS3),
        properties("s3", "bucket")
    );

    assertThat(registry.writeStorage()).isSameAs(s3);
    assertThat(registry.privateItemClaimStorage()).isSameAs(privateS3);
    assertThat(registry.get("local")).isSameAs(local);
  }

  @Test
  void missingS3ConfigurationFailsFastWhenS3IsWriteStorage() {
    FileStorageRegistry localRegistry = new FileStorageRegistry(
        List.of(storage("LOCAL"), storage("S3")),
        properties("LOCAL", "")
    );

    assertThat(localRegistry.writeStorage().provider())
        .isEqualTo("LOCAL");
    assertThatThrownBy(() -> new FileStorageRegistry(
        List.of(storage("LOCAL"), storage("S3")),
        properties("S3", "")
    ))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("S3_BUCKET");
  }

  @Test
  void missingCloudFrontUrlFailsFastWhenS3IsWriteStorage() {
    FileStorageProperties properties = new FileStorageProperties(
        "S3",
        new FileStorageProperties.S3(
            "bucket",
            "ap-northeast-2",
            "",
            Duration.ofMinutes(10)
        )
    );

    assertThatThrownBy(() -> new FileStorageRegistry(
        List.of(storage("LOCAL"), storage("S3")),
        properties
    ))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CLOUDFRONT_BASE_URL");
  }

  private FileStorage storage(String provider) {
    FileStorage storage = mock(FileStorage.class);
    when(storage.provider()).thenReturn(provider);
    return storage;
  }

  private FileStorageProperties properties(
      String provider,
      String bucket
  ) {
    return new FileStorageProperties(
        provider,
        new FileStorageProperties.S3(
            bucket,
            "ap-northeast-2",
            "https://example.cloudfront.net",
            Duration.ofMinutes(10)
        )
    );
  }
}
