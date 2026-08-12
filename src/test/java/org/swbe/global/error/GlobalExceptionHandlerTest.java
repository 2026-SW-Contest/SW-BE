package org.swbe.global.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    GlobalExceptionHandler.class,
    GlobalExceptionHandlerTest.TestController.class
})
class GlobalExceptionHandlerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void requestBodyValidationFailureReturnsFieldErrors() throws Exception {
    mockMvc.perform(post("/test/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": ""
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"))
        .andExpect(jsonPath("$.message").value("요청 값 검증에 실패했습니다."))
        .andExpect(jsonPath("$.path").value("/test/users"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
        .andExpect(jsonPath("$.fieldErrors[0].message").value("이름은 필수입니다."));
  }

  @Test
  void malformedJsonReturnsBadRequest() throws Exception {
    mockMvc.perform(post("/test/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name":
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_MALFORMED_JSON"))
        .andExpect(jsonPath("$.path").value("/test/users"))
        .andExpect(jsonPath("$.fieldErrors").isEmpty());
  }

  @Test
  void businessExceptionUsesDefinedErrorCode() throws Exception {
    mockMvc.perform(get("/test/business-error"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("COMMON_RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.message")
            .value("요청한 리소스를 찾을 수 없습니다."))
        .andExpect(jsonPath("$.path").value("/test/business-error"));
  }

  @Test
  void unknownPathReturnsResourceNotFound() throws Exception {
    mockMvc.perform(get("/unknown"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("COMMON_RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.path").value("/unknown"));
  }

  @Test
  void unexpectedExceptionDoesNotExposeInternalMessage() throws Exception {
    mockMvc.perform(get("/test/unexpected-error"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("COMMON_INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
        .andExpect(jsonPath("$.message").value(
            org.hamcrest.Matchers.not("sensitive internal message")
        ));
  }

  @Test
  void oversizedUploadReturnsPayloadTooLarge() throws Exception {
    mockMvc.perform(get("/test/upload-too-large"))
        .andExpect(status().is(413))
        .andExpect(jsonPath("$.status").value(413))
        .andExpect(jsonPath("$.code")
            .value("COMMON_UPLOAD_SIZE_EXCEEDED"))
        .andExpect(jsonPath("$.path")
            .value("/test/upload-too-large"));
  }

  @RestController
  @RequestMapping("/test")
  public static class TestController {

    @PostMapping("/users")
    void createUser(@Valid @RequestBody TestRequest request) {
    }

    @GetMapping("/business-error")
    void businessError() {
      throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    @GetMapping("/unexpected-error")
    void unexpectedError() {
      throw new IllegalStateException("sensitive internal message");
    }

    @GetMapping("/upload-too-large")
    void uploadTooLarge() {
      throw new MaxUploadSizeExceededException(10L);
    }
  }

  record TestRequest(
      @NotBlank(message = "이름은 필수입니다.")
      String name
  ) {
  }
}
