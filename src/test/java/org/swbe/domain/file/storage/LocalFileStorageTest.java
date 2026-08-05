package org.swbe.domain.file.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalFileStorageTest {

  @TempDir
  private Path temporaryDirectory;

  @Test
  void storesAndDeletesFileInsideConfiguredDirectory() throws Exception {
    Clock clock = Clock.fixed(
        Instant.parse("2026-08-01T07:00:00Z"),
        ZoneOffset.UTC
    );
    LocalFileStorage storage = new LocalFileStorage(
        temporaryDirectory.toString(),
        clock
    );
    MockMultipartFile image = new MockMultipartFile(
        "files",
        "broken-light.jpg",
        "image/jpeg",
        "image-content".getBytes(StandardCharsets.UTF_8)
    );

    StoredFile storedFile = storage.store(image);

    Path storedPath = temporaryDirectory.resolve(storedFile.storageKey());
    assertThat(storedFile.storageProvider()).isEqualTo("LOCAL");
    assertThat(storedFile.storageKey()).startsWith("2026/08/01/");
    assertThat(storedFile.originalFilename()).isEqualTo("broken-light.jpg");
    assertThat(storedFile.mimeType()).isEqualTo("image/jpeg");
    assertThat(storedFile.checksum()).hasSize(64);
    assertThat(Files.readString(storedPath)).isEqualTo("image-content");

    storage.delete(storedFile.storageKey());

    assertThat(storedPath).doesNotExist();
  }
}
