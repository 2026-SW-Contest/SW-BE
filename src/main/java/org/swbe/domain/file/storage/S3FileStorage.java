package org.swbe.domain.file.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.swbe.domain.file.config.FileStorageProperties;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
public class S3FileStorage implements FileStorage {

  private static final String STORAGE_PROVIDER = "S3";
  private static final DateTimeFormatter DIRECTORY_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy/MM/dd");

  private final S3Client s3Client;
  private final FileStorageProperties properties;
  private final Clock clock;

  @Override
  public String provider() {
    return STORAGE_PROVIDER;
  }

  @Override
  public StoredFile store(MultipartFile file) {
    String originalFilename = safeOriginalFilename(
        file.getOriginalFilename()
    );
    String storageKey = createStorageKey(originalFilename);
    String mimeType = Objects.requireNonNullElse(
        file.getContentType(),
        "application/octet-stream"
    );
    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(properties.s3().bucket())
        .key(storageKey)
        .contentType(mimeType)
        .build();

    try (InputStream input = file.getInputStream()) {
      s3Client.putObject(
          request,
          RequestBody.fromInputStream(input, file.getSize())
      );
      return new StoredFile(
          STORAGE_PROVIDER,
          storageKey,
          originalFilename,
          mimeType,
          file.getSize(),
          null
      );
    } catch (IOException | SdkException exception) {
      throw new FileStorageException(
          "Failed to store S3 object",
          exception
      );
    }
  }

  @Override
  public void delete(String storageKey) {
    DeleteObjectRequest request = DeleteObjectRequest.builder()
        .bucket(properties.s3().bucket())
        .key(storageKey)
        .build();
    try {
      s3Client.deleteObject(request);
    } catch (SdkException exception) {
      throw new FileStorageException(
          "Failed to delete S3 object",
          exception
      );
    }
  }

  private String createStorageKey(String originalFilename) {
    String extension = extensionOf(originalFilename);
    String directory = LocalDate.now(clock).format(DIRECTORY_FORMATTER);
    return directory + "/" + UUID.randomUUID() + extension;
  }

  private String safeOriginalFilename(String originalFilename) {
    if (originalFilename == null || originalFilename.isBlank()) {
      return "image";
    }
    return Path.of(originalFilename).getFileName().toString();
  }

  private String extensionOf(String filename) {
    int dotIndex = filename.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex == filename.length() - 1) {
      return "";
    }
    return filename.substring(dotIndex).toLowerCase(Locale.ROOT);
  }
}
