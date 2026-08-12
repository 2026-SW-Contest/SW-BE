package org.swbe.domain.file.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class S3Config {

  @Bean
  public S3Client s3Client(FileStorageProperties properties) {
    return S3Client.builder()
        .region(Region.of(properties.s3().region()))
        .build();
  }
}
