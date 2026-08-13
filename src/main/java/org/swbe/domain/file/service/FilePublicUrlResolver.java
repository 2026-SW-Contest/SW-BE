package org.swbe.domain.file.service;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.swbe.domain.file.config.FileStorageProperties;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.exception.FileErrorCode;
import org.swbe.global.error.BusinessException;

@Component
@RequiredArgsConstructor
public class FilePublicUrlResolver {

  private final FileStorageProperties properties;

  public String resolve(FileResource file) {
    return switch (normalize(file.getStorageProvider())) {
      case "S3" -> cloudFrontUrl(file.getStorageKey());
      default -> throw new BusinessException(
          FileErrorCode.STORAGE_PROVIDER_NOT_SUPPORTED
      );
    };
  }

  private String cloudFrontUrl(String storageKey) {
    return UriComponentsBuilder
        .fromUriString(properties.s3().cloudfrontBaseUrl())
        .pathSegment(storageKey.split("/"))
        .build()
        .encode()
        .toUriString();
  }

  private String normalize(String provider) {
    return provider == null
        ? ""
        : provider.strip().toUpperCase(Locale.ROOT);
  }
}
