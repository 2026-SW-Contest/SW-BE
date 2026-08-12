package org.swbe.domain.file.controller;

import jakarta.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.swbe.domain.file.dto.FileDownload;
import org.swbe.domain.file.service.FileQueryService;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Validated
public class FileController {

  private final FileQueryService fileQueryService;

  @GetMapping("/{fileId}")
  public ResponseEntity<Resource> getLocalImage(
      @PathVariable @Positive Long fileId
  ) {
    FileDownload file = fileQueryService.getLocalPublicImage(fileId);

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.mimeType()))
        .contentLength(file.size())
        .cacheControl(
            CacheControl.maxAge(Duration.ofHours(1)).cachePublic()
        )
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline()
                .filename(
                    file.originalFilename(),
                    StandardCharsets.UTF_8
                )
                .build()
                .toString()
        )
        .header("X-Content-Type-Options", "nosniff")
        .body(file.resource());
  }
}
