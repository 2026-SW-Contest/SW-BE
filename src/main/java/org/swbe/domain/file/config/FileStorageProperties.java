package org.swbe.domain.file.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.file")
public record FileStorageProperties(
    String storageProvider,
    S3 s3
) {

  public record S3(
      String bucket,
      String region,
      String cloudfrontBaseUrl,
      Duration privateUrlValidity
  ) {
  }
}
