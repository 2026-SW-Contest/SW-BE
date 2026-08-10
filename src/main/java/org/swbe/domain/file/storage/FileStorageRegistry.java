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
  private final String writeProvider;

  public FileStorageRegistry(
      List<FileStorage> storages,
      FileStorageProperties properties
  ) {
    this.storages = storages.stream()
        .collect(Collectors.toUnmodifiableMap(
            storage -> normalize(storage.provider()),
            Function.identity()
        ));
    this.writeProvider = normalize(properties.storageProvider());
    validateWriteStorage(properties);
  }

  public FileStorage writeStorage() {
    return get(writeProvider);
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

  private void validateWriteStorage(FileStorageProperties properties) {
    get(writeProvider);
    if (!"S3".equals(writeProvider)) {
      return;
    }

    FileStorageProperties.S3 s3 = properties.s3();
    if (s3 == null
        || s3.bucket() == null
        || s3.bucket().isBlank()
        || s3.region() == null
        || s3.region().isBlank()) {
      throw new IllegalStateException(
          "S3_BUCKET and AWS_REGION are required for S3 storage"
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
