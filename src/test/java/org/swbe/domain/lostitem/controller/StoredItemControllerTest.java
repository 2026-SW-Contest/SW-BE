package org.swbe.domain.lostitem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.swbe.domain.lostitem.dto.response.StoredItemCategoryResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemDetailDataResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemDetailResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemListItemResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemListResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemOfficeResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemSliceResponse;
import org.swbe.domain.lostitem.service.StoredItemDetailService;
import org.swbe.domain.lostitem.service.StoredItemQueryService;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(StoredItemController.class)
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
class StoredItemControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private StoredItemQueryService storedItemQueryService;

  @MockitoBean
  private StoredItemDetailService storedItemDetailService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void anonymousUserCanGetFilteredStoredItemList() throws Exception {
    StoredItemListItemResponse item = new StoredItemListItemResponse(
        25L,
        "검은색 지갑",
        "학생증과 카드가 들어 있습니다.",
        "지갑/카드/현금",
        "명진관 2층",
        LocalDate.of(2026, 8, 10),
        "STORED",
        "보관중",
        "/api/files/31",
        LocalDateTime.of(2026, 8, 10, 14, 30)
    );
    when(storedItemQueryService.getStoredItems(any())).thenReturn(
        new StoredItemListResponse(
            new StoredItemSliceResponse(
                List.of(item),
                "next-cursor",
                true
            )
        )
    );

    mockMvc.perform(get("/api/stored-items")
            .queryParam("categoryId", "2")
            .queryParam("locationId", "10")
            .queryParam("status", "STORED")
            .queryParam("from", "2026-08-01")
            .queryParam("to", "2026-08-12")
            .queryParam("cursor", "cursor")
            .queryParam("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].storedItemId").value(25))
        .andExpect(jsonPath("$.data.content[0].description")
            .value("학생증과 카드가 들어 있습니다."))
        .andExpect(jsonPath("$.data.content[0].publicDescription")
            .doesNotExist())
        .andExpect(jsonPath("$.data.content[0].publicStatusName")
            .value("보관중"))
        .andExpect(jsonPath("$.data.nextCursor").value("next-cursor"))
        .andExpect(jsonPath("$.data.hasNext").value(true));
  }

  @Test
  void anonymousUserCanGetStoredItemDetail() throws Exception {
    StoredItemDetailDataResponse data = new StoredItemDetailDataResponse(
        25L,
        "검은색 지갑",
        "공개 설명",
        new StoredItemCategoryResponse(2L, "지갑/카드/현금"),
        null,
        LocalDate.of(2026, 8, 10),
        null,
        true,
        "STORED",
        "보관중",
        new StoredItemOfficeResponse(3L, "본관 경비실"),
        List.of(),
        LocalDateTime.of(2026, 8, 10, 14, 30),
        LocalDateTime.of(2026, 8, 10, 14, 30)
    );
    when(storedItemDetailService.getStoredItem(25L))
        .thenReturn(new StoredItemDetailResponse(data));

    mockMvc.perform(get("/api/stored-items/25"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.storedItemId").value(25))
        .andExpect(jsonPath("$.data.description").value("공개 설명"))
        .andExpect(jsonPath("$.data.office.officeId").value(3))
        .andExpect(jsonPath("$.data.foundLocation").isEmpty())
        .andExpect(jsonPath("$.data.attachments.length()").value(0))
        .andExpect(jsonPath("$.data.privateDescription").doesNotExist())
        .andExpect(jsonPath("$.data.storagePosition").doesNotExist())
        .andExpect(jsonPath("$.data.editable").doesNotExist())
        .andExpect(jsonPath("$.data.deletable").doesNotExist());
  }

  @Test
  void rejectsSizeGreaterThanFifty() throws Exception {
    mockMvc.perform(get("/api/stored-items")
            .queryParam("size", "51"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code")
            .value("COMMON_VALIDATION_FAILED"));
  }

  @Test
  void rejectsNonPositiveStoredItemId() throws Exception {
    mockMvc.perform(get("/api/stored-items/0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code")
            .value("COMMON_VALIDATION_FAILED"));
  }
}
