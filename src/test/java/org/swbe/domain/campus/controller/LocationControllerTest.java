package org.swbe.domain.campus.controller;

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
import org.swbe.domain.campus.dto.response.LocationListResponse;
import org.swbe.domain.campus.dto.response.LocationResponse;
import org.swbe.domain.campus.service.LocationQueryService;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(LocationController.class)
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
class LocationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private LocationQueryService locationQueryService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void anonymousUserCanGetLocationList() throws Exception {
    when(locationQueryService.getLocations()).thenReturn(
        new LocationListResponse(List.of(
            new LocationResponse(1L, "S1", "본관(종합관)"),
            new LocationResponse(11L, null, "기타")
        ))
    );

    mockMvc.perform(get("/api/locations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].locationId").value(1))
        .andExpect(jsonPath("$.data[0].locationCode").value("S1"))
        .andExpect(jsonPath("$.data[0].locationName")
            .value("본관(종합관)"))
        .andExpect(jsonPath("$.data[1].locationCode").isEmpty())
        .andExpect(jsonPath("$.data[1].locationName").value("기타"));
  }
}
