package org.swbe.domain.lostitem.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.swbe.domain.lostitem.service.StoredItemDeleteService;
import org.swbe.domain.user.entity.AccountStatus;
import org.swbe.global.security.AppUserPrincipal;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(StoredItemDeleteController.class)
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
class StoredItemDeleteControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private StoredItemDeleteService storedItemDeleteService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void lostItemStaffCanDeleteStoredItem() throws Exception {
    mockMvc.perform(delete("/api/stored-items/25")
            .with(user(principal("ROLE_LOST_ITEM_STAFF")))
            .with(csrf()))
        .andExpect(status().isNoContent());

    verify(storedItemDeleteService).delete(25L, 7L, false);
  }

  @Test
  void adminFlagIsPassedToService() throws Exception {
    mockMvc.perform(delete("/api/stored-items/25")
            .with(user(principal("ROLE_ADMIN")))
            .with(csrf()))
        .andExpect(status().isNoContent());

    verify(storedItemDeleteService).delete(25L, 7L, true);
  }

  @Test
  void studentCannotDeleteStoredItem() throws Exception {
    mockMvc.perform(delete("/api/stored-items/25")
            .with(user(principal("ROLE_STUDENT")))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void anonymousUserCannotDeleteStoredItem() throws Exception {
    mockMvc.perform(delete("/api/stored-items/25")
            .with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void nonPositiveStoredItemIdIsRejected() throws Exception {
    mockMvc.perform(delete("/api/stored-items/0")
            .with(user(principal("ROLE_LOST_ITEM_STAFF")))
            .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  private AppUserPrincipal principal(String authority) {
    return new AppUserPrincipal(
        7L,
        "staff@mju.ac.kr",
        "{noop}password",
        AccountStatus.ACTIVE,
        true,
        List.of(new SimpleGrantedAuthority(authority))
    );
  }
}
