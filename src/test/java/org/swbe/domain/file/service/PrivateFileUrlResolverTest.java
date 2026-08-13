package org.swbe.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.swbe.domain.file.config.FileStorageProperties;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.exception.FileErrorCode;
import org.swbe.global.error.BusinessException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class PrivateFileUrlResolverTest {

  @Test
  void createsTenMinutePresignedUrlForPrivateS3File() throws Exception {
    S3Presigner presigner = mock(S3Presigner.class);
    PresignedGetObjectRequest signed = mock(
        PresignedGetObjectRequest.class
    );
    when(signed.url()).thenReturn(
        URI.create("https://s3.example.com/signed").toURL()
    );
    when(presigner.presignGetObject(any(GetObjectPresignRequest.class)))
        .thenReturn(signed);
    PrivateFileUrlResolver resolver = new PrivateFileUrlResolver(
        presigner,
        properties()
    );
    FileResource file = file(
        "S3_PRIVATE",
        "private/item-claims/2026/08/12/proof.jpg"
    );

    assertThat(resolver.resolve(file))
        .isEqualTo("https://s3.example.com/signed");

    ArgumentCaptor<GetObjectPresignRequest> request =
        ArgumentCaptor.forClass(GetObjectPresignRequest.class);
    verify(presigner).presignGetObject(request.capture());
    assertThat(request.getValue().signatureDuration())
        .isEqualTo(Duration.ofMinutes(10));
    GetObjectRequest objectRequest = request.getValue().getObjectRequest();
    assertThat(objectRequest.bucket()).isEqualTo("test-bucket");
    assertThat(objectRequest.key())
        .isEqualTo("private/item-claims/2026/08/12/proof.jpg");
  }

  @Test
  void rejectsPublicS3File() {
    PrivateFileUrlResolver resolver = new PrivateFileUrlResolver(
        mock(S3Presigner.class),
        properties()
    );

    assertThatThrownBy(() -> resolver.resolve(
        file("S3", "public/2026/08/12/item.jpg")
    )).isInstanceOfSatisfying(
        BusinessException.class,
        exception -> assertThat(exception.getErrorCode())
            .isEqualTo(FileErrorCode.STORAGE_PROVIDER_NOT_SUPPORTED)
    );
  }

  private FileStorageProperties properties() {
    return new FileStorageProperties(
        new FileStorageProperties.S3(
            "test-bucket",
            "ap-northeast-2",
            "https://cdn.example.com",
            Duration.ofMinutes(10)
        )
    );
  }

  private FileResource file(String provider, String key) {
    FileResource file = mock(FileResource.class);
    when(file.getStorageProvider()).thenReturn(provider);
    when(file.getStorageKey()).thenReturn(key);
    return file;
  }
}
