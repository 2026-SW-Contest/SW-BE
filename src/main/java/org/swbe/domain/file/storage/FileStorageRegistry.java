package org.swbe.domain.file.storage;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.swbe.domain.file.config.FileStorageProperties;

@Component
public class FileStorageRegistry {

  private final Map<String, FileStorage> storages;

  public FileStorageRegistry(
      List<FileStorage> storages,
      FileStorageProperties properties
  ) {
    this.storages = storages.stream()
        .collect(Collectors.toUnmodifiableMap(
            storage -> normalize(storage.provider()),
            Function.identity()
        ));
    validateS3Storage(properties);
  }

  public FileStorage writeStorage() {
    return get("S3");
  }

  public FileStorage privateItemClaimStorage() {
    return get("S3_PRIVATE");
  }

  public FileStorage get(String provider) {
    FileStorage storage = storages.get(normalize(provider));
    if (storage == null) {
      throw new IllegalStateException(
          "Unsupported file storage provider: " + provider
      );
    }
    return storage;
  }

  private void validateS3Storage(FileStorageProperties properties) {
    get("S3");
    FileStorageProperties.S3 s3 = properties.s3();
    if (s3 == null
        || s3.bucket() == null
        || s3.bucket().isBlank()
        || s3.region() == null
        || s3.region().isBlank()
        || s3.cloudfrontBaseUrl() == null
        || s3.cloudfrontBaseUrl().isBlank()) {
      throw new IllegalStateException(
          "S3_BUCKET, AWS_REGION and CLOUDFRONT_BASE_URL are required"
      );
    }
  }

  private String normalize(String provider) {
    if (provider == null || provider.isBlank()) {
      throw new IllegalStateException(
          "File storage provider must not be blank"
      );
    }
    return provider.strip().toUpperCase(Locale.ROOT);
  }
}
