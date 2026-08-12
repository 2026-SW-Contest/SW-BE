package org.swbe.domain.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.swbe.domain.file.config.FileStorageProperties;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class S3FileStorageTest {

  private S3Client s3Client;
  private S3FileStorage storage;

  @BeforeEach
  void setUp() {
    s3Client = mock(S3Client.class);
    FileStorageProperties properties = new FileStorageProperties(
        "S3",
        new FileStorageProperties.S3(
            "test-bucket",
            "ap-northeast-2",
            "https://example.cloudfront.net"
        )
    );
    Clock clock = Clock.fixed(
        Instant.parse("2026-08-10T03:00:00Z"),
        ZoneOffset.UTC
    );
    storage = new S3FileStorage(s3Client, properties, clock);
  }

  @Test
  void storesImageInConfiguredBucket() {
    MockMultipartFile image = new MockMultipartFile(
        "files",
        "airpods.JPG",
        "image/jpeg",
        "image-content".getBytes(StandardCharsets.UTF_8)
    );
    when(s3Client.putObject(
        any(PutObjectRequest.class),
        any(RequestBody.class)
    )).thenReturn(PutObjectResponse.builder().build());

    StoredFile storedFile = storage.store(image);

    assertThat(storedFile.storageProvider()).isEqualTo("S3");
    assertThat(storedFile.storageKey())
        .startsWith("2026/08/10/")
        .endsWith(".jpg");
    assertThat(storedFile.originalFilename()).isEqualTo("airpods.JPG");
    assertThat(storedFile.mimeType()).isEqualTo("image/jpeg");
    assertThat(storedFile.size()).isEqualTo(image.getSize());

    ArgumentCaptor<PutObjectRequest> requestCaptor =
        ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(
        requestCaptor.capture(),
        any(RequestBody.class)
    );
    assertThat(requestCaptor.getValue().bucket())
        .isEqualTo("test-bucket");
    assertThat(requestCaptor.getValue().key())
        .isEqualTo(storedFile.storageKey());
    assertThat(requestCaptor.getValue().contentType())
        .isEqualTo("image/jpeg");
  }

  @Test
  void deletesObjectFromConfiguredBucket() {
    when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenReturn(DeleteObjectResponse.builder().build());

    storage.delete("2026/08/10/image.jpg");

    ArgumentCaptor<DeleteObjectRequest> requestCaptor =
        ArgumentCaptor.forClass(DeleteObjectRequest.class);
    verify(s3Client).deleteObject(requestCaptor.capture());
    assertThat(requestCaptor.getValue().bucket())
        .isEqualTo("test-bucket");
    assertThat(requestCaptor.getValue().key())
        .isEqualTo("2026/08/10/image.jpg");
  }
}
