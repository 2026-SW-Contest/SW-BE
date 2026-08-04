package org.swbe.domain.servicerequest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.swbe.domain.servicerequest.dto.response.FacilityCategoryListResponse;
import org.swbe.domain.servicerequest.dto.response.FacilityCategoryResponse;
import org.swbe.domain.servicerequest.service.FacilityCategoryQueryService;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(FacilityCategoryController.class)
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
class FacilityCategoryControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private FacilityCategoryQueryService facilityCategoryQueryService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void anonymousUserCanGetCategoryList() throws Exception {
    when(facilityCategoryQueryService.getCategories()).thenReturn(
        new FacilityCategoryListResponse(List.of(
            new FacilityCategoryResponse(1L, "전기/조명"),
            new FacilityCategoryResponse(8L, "기타")
        ))
    );

    mockMvc.perform(get("/api/facility-categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].categoryId").value(1))
        .andExpect(jsonPath("$.data[0].categoryName").value("전기/조명"))
        .andExpect(jsonPath("$.data[1].categoryId").value(8))
        .andExpect(jsonPath("$.data[1].categoryName").value("기타"));
  }
}
