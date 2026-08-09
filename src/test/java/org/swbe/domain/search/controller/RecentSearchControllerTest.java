package org.swbe.domain.search.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.swbe.domain.search.dto.response.RecentSearchListResponse;
import org.swbe.domain.search.dto.response.RecentSearchResponse;
import org.swbe.domain.search.service.RecentSearchService;
import org.swbe.domain.user.entity.AccountStatus;
import org.swbe.global.security.AppUserPrincipal;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(RecentSearchController.class)
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
class RecentSearchControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RecentSearchService recentSearchService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void anonymousUserCannotGetRecentSearches() throws Exception {
    mockMvc.perform(get("/api/recent-searches"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code")
            .value("SECURITY_AUTHENTICATION_REQUIRED"));
  }

  @Test
  void authenticatedUserCanGetOwnRecentSearches() throws Exception {
    when(recentSearchService.getRecentSearches(10L)).thenReturn(
        new RecentSearchListResponse(List.of(
            new RecentSearchResponse(
                1L,
                "에어",
                LocalDateTime.of(2026, 8, 8, 12, 0)
            )
        ))
    );

    mockMvc.perform(get("/api/recent-searches")
            .session(authenticatedSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].recentSearchId").value(1))
        .andExpect(jsonPath("$.data[0].keyword").value("에어"));
  }

  @Test
  void recordingRecentSearchRequiresCsrfToken() throws Exception {
    mockMvc.perform(post("/api/recent-searches")
            .session(authenticatedSession())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "keyword": "에어"
                }
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code")
            .value("SECURITY_INVALID_CSRF_TOKEN"));
  }

  @Test
  void authenticatedUserCanRecordRecentSearch() throws Exception {
    when(recentSearchService.record(eq(10L), any())).thenReturn(
        new RecentSearchListResponse(List.of(
            new RecentSearchResponse(
                1L,
                "에어",
                LocalDateTime.of(2026, 8, 8, 12, 0)
            )
        ))
    );

    mockMvc.perform(post("/api/recent-searches")
            .session(authenticatedSession())
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "keyword": "에어"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].keyword").value("에어"));
  }

  private MockHttpSession authenticatedSession() {
    AppUserPrincipal principal = new AppUserPrincipal(
        10L,
        "student@mju.ac.kr",
        "{bcrypt}password-hash",
        AccountStatus.ACTIVE,
        true,
        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
    );
    var authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            principal.getAuthorities()
        );
    SecurityContext context =
        SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);

    MockHttpSession session = new MockHttpSession();
    session.setAttribute(
        HttpSessionSecurityContextRepository
            .SPRING_SECURITY_CONTEXT_KEY,
        context
    );
    return session;
  }
}
