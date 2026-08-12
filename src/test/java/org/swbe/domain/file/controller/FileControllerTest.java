package org.swbe.domain.file.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.swbe.domain.file.dto.FileDownload;
import org.swbe.domain.file.service.FileQueryService;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(FileController.class)
@Import({
    SecurityConfig.class,
    SecurityErrorResponseWriter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    RestSessionInformationExpiredStrategy.class
})
@TestPropertySource(properties = {
    "app.security.frontend-origins[0]=http://localhost:3000"
})
class FileControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private FileQueryService fileQueryService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void anonymousUserCanGetLocalImageInline() throws Exception {
    byte[] content = "image".getBytes(StandardCharsets.UTF_8);
    when(fileQueryService.getLocalPublicImage(15L)).thenReturn(
        new FileDownload(
            new ByteArrayResource(content),
            "고장 조명.jpg",
            "image/jpeg",
            content.length
        )
    );

    mockMvc.perform(get("/api/files/{fileId}", 15L))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/jpeg"))
        .andExpect(content().bytes(content))
        .andExpect(header().string(
            HttpHeaders.CACHE_CONTROL,
            "max-age=3600, public"
        ))
        .andExpect(header().string(
            "X-Content-Type-Options",
            "nosniff"
        ));
  }

  @Test
  void rejectsNonPositiveFileId() throws Exception {
    mockMvc.perform(get("/api/files/{fileId}", 0))
        .andExpect(status().isBadRequest());
  }
}
