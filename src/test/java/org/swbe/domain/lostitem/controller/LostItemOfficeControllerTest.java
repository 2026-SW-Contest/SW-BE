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
import org.swbe.domain.lostitem.dto.response.LostItemOfficeListResponse;
import org.swbe.domain.lostitem.dto.response.LostItemOfficeResponse;
import org.swbe.domain.lostitem.service.LostItemOfficeQueryService;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(LostItemOfficeController.class)
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
class LostItemOfficeControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private LostItemOfficeQueryService officeQueryService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void anonymousUserCanGetOfficeList() throws Exception {
    when(officeQueryService.getOffices()).thenReturn(
        new LostItemOfficeListResponse(List.of(
            new LostItemOfficeResponse(
                1L,
                "인문학생지원팀 분실물 보관소",
                1L,
                "S1",
                "본관(종합관)",
                12L,
                "인문학생지원팀",
                "2층",
                "인문학생지원팀",
                "평일 09:00~17:30",
                "분실물 관련 문의: 02-300-1521",
                true
            )
        ))
    );

    mockMvc.perform(get("/api/lost-item-offices"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].officeId").value(1))
        .andExpect(jsonPath("$.data[0].officeName")
            .value("인문학생지원팀 분실물 보관소"))
        .andExpect(jsonPath("$.data[0].buildingCode").value("S1"))
        .andExpect(jsonPath("$.data[0].locationName")
            .value("인문학생지원팀"))
        .andExpect(jsonPath("$.data[0].departmentName")
            .value("인문학생지원팀"))
        .andExpect(jsonPath("$.data[0].primary").value(true));
  }
}
