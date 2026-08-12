package org.swbe.domain.file.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.swbe.domain.file.config.FileStorageProperties;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.exception.FileErrorCode;
import org.swbe.global.error.BusinessException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class PrivateFileUrlResolver {

  private static final String PRIVATE_PROVIDER = "S3_PRIVATE";

  private final S3Presigner s3Presigner;
  private final FileStorageProperties properties;

  public String resolve(FileResource file) {
    if (!PRIVATE_PROVIDER.equals(file.getStorageProvider())) {
      throw new BusinessException(
          FileErrorCode.STORAGE_PROVIDER_NOT_SUPPORTED
      );
    }
    Duration validity = properties.s3().privateUrlValidity();
    GetObjectRequest objectRequest = GetObjectRequest.builder()
        .bucket(properties.s3().bucket())
        .key(file.getStorageKey())
        .build();
    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(validity)
        .getObjectRequest(objectRequest)
        .build();

    return s3Presigner.presignGetObject(presignRequest)
        .url()
        .toString();
  }
}
