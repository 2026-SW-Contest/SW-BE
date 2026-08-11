package org.swbe.domain.lostitem.controller;

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
import org.swbe.domain.lostitem.dto.response.ItemCategoryListResponse;
import org.swbe.domain.lostitem.dto.response.ItemCategoryResponse;
import org.swbe.domain.lostitem.service.ItemCategoryQueryService;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(ItemCategoryController.class)
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
class ItemCategoryControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ItemCategoryQueryService itemCategoryQueryService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void anonymousUserCanGetItemCategoryList() throws Exception {
    when(itemCategoryQueryService.getCategories()).thenReturn(
        new ItemCategoryListResponse(List.of(
            new ItemCategoryResponse(1L, "전자기기"),
            new ItemCategoryResponse(6L, "액세서리"),
            new ItemCategoryResponse(7L, "기타")
        ))
    );

    mockMvc.perform(get("/api/item-categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(3))
        .andExpect(jsonPath("$.data[0].categoryId").value(1))
        .andExpect(jsonPath("$.data[0].categoryName").value("전자기기"))
        .andExpect(jsonPath("$.data[1].categoryId").value(6))
        .andExpect(jsonPath("$.data[1].categoryName").value("액세서리"))
        .andExpect(jsonPath("$.data[2].categoryId").value(7))
        .andExpect(jsonPath("$.data[2].categoryName").value("기타"));
  }
}
