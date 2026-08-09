package org.swbe.domain.file.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalFileStorage implements FileStorage {

  private static final String STORAGE_PROVIDER = "LOCAL";
  private static final DateTimeFormatter DIRECTORY_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy/MM/dd");

  private final Path rootDirectory;
  private final Clock clock;

  public LocalFileStorage(
      @Value("${app.file.storage-directory:uploads}") String directory,
      Clock clock
  ) {
    this.rootDirectory = Path.of(directory).toAbsolutePath().normalize();
    this.clock = clock;
  }

  @Override
  public StoredFile store(MultipartFile file) {
    String originalFilename = safeOriginalFilename(
        file.getOriginalFilename()
    );
    String storageKey = createStorageKey(originalFilename);
    Path target = resolveStorageKey(storageKey);

    try {
      Files.createDirectories(Objects.requireNonNull(target.getParent()));
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (
          InputStream input = new DigestInputStream(
              file.getInputStream(),
              digest
          );
          OutputStream output = Files.newOutputStream(
              target,
              StandardOpenOption.CREATE_NEW,
              StandardOpenOption.WRITE
          )
      ) {
        input.transferTo(output);
      }

      return new StoredFile(
          STORAGE_PROVIDER,
          storageKey,
          originalFilename,
          Objects.requireNonNullElse(
              file.getContentType(),
              "application/octet-stream"
          ),
          file.getSize(),
          HexFormat.of().formatHex(digest.digest())
      );
    } catch (IOException | NoSuchAlgorithmException exception) {
      deleteQuietly(target);
      throw new FileStorageException("Failed to store file", exception);
    }
  }

  @Override
  public void delete(String storageKey) {
    Path target = resolveStorageKey(storageKey);
    try {
      Files.deleteIfExists(target);
    } catch (IOException exception) {
      throw new FileStorageException("Failed to delete file", exception);
    }
  }

  private String createStorageKey(String originalFilename) {
    String extension = extensionOf(originalFilename);
    String directory = LocalDate.now(clock).format(DIRECTORY_FORMATTER);
    return directory + "/" + UUID.randomUUID() + extension;
  }

  private Path resolveStorageKey(String storageKey) {
    Path resolved = rootDirectory.resolve(storageKey).normalize();
    if (!resolved.startsWith(rootDirectory)) {
      throw new IllegalArgumentException("Invalid storage key");
    }
    return resolved;
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
    return filename.substring(dotIndex).toLowerCase();
  }

  private void deleteQuietly(Path target) {
    try {
      Files.deleteIfExists(target);
    } catch (IOException ignored) {
      // The original storage failure is more useful to the caller.
    }
  }
}
