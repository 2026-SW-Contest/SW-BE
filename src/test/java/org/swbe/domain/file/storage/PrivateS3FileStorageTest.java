package org.swbe.domain.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
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

class PrivateS3FileStorageTest {

  private S3Client s3Client;
  private PrivateS3FileStorage storage;

  @BeforeEach
  void setUp() {
    s3Client = mock(S3Client.class);
    FileStorageProperties properties = new FileStorageProperties(
        new FileStorageProperties.S3(
            "test-bucket",
            "ap-northeast-2",
            "https://example.cloudfront.net",
            Duration.ofMinutes(10)
        )
    );
    storage = new PrivateS3FileStorage(
        s3Client,
        properties,
        Clock.fixed(
            Instant.parse("2026-08-12T03:00:00Z"),
            ZoneOffset.UTC
        )
    );
  }

  @Test
  void storesClaimEvidenceUnderPrivatePrefix() {
    MockMultipartFile image = new MockMultipartFile(
        "files",
        "proof.JPG",
        "image/jpeg",
        "image-content".getBytes(StandardCharsets.UTF_8)
    );
    when(s3Client.putObject(
        any(PutObjectRequest.class),
        any(RequestBody.class)
    )).thenReturn(PutObjectResponse.builder().build());

    StoredFile storedFile = storage.store(image);

    assertThat(storedFile.storageProvider()).isEqualTo("S3_PRIVATE");
    assertThat(storedFile.storageKey())
        .startsWith("private/item-claims/2026/08/12/")
        .endsWith(".jpg");
    ArgumentCaptor<PutObjectRequest> request =
        ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(request.capture(), any(RequestBody.class));
    assertThat(request.getValue().bucket()).isEqualTo("test-bucket");
    assertThat(request.getValue().key())
        .isEqualTo(storedFile.storageKey());
  }

  @Test
  void deletesPrivateObjectFromConfiguredBucket() {
    when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenReturn(DeleteObjectResponse.builder().build());

    storage.delete("private/item-claims/2026/08/12/proof.jpg");

    ArgumentCaptor<DeleteObjectRequest> request =
        ArgumentCaptor.forClass(DeleteObjectRequest.class);
    verify(s3Client).deleteObject(request.capture());
    assertThat(request.getValue().bucket()).isEqualTo("test-bucket");
    assertThat(request.getValue().key())
        .isEqualTo("private/item-claims/2026/08/12/proof.jpg");
  }
}
