package org.swbe.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

class SecurityConfigTest {

  @Test
  void commaSeparatedPropertyBindsToFrontendOriginList() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty(
            "app.security.frontend-origins",
            "https://www.example.com,http://localhost:3000"
        );

    SecurityProperties properties = Binder.get(environment)
        .bind("app.security", Bindable.of(SecurityProperties.class))
        .orElseThrow(() -> new AssertionError("Security properties were not bound"));

    assertThat(properties.frontendOrigins()).containsExactly(
        "https://www.example.com",
        "http://localhost:3000"
    );
  }

  @Test
  void corsConfigurationAllowsAllConfiguredFrontendOrigins() {
    List<String> frontendOrigins = List.of(
        "https://www.example.com",
        "http://localhost:3000"
    );
    SecurityProperties properties = new SecurityProperties(frontendOrigins);

    UrlBasedCorsConfigurationSource source =
        new SecurityConfig().corsConfigurationSource(properties);
    CorsConfiguration configuration = source.getCorsConfiguration(
        new MockHttpServletRequest("GET", "/api/auth/csrf")
    );

    assertThat(configuration).isNotNull();
    assertThat(configuration.getAllowedOrigins())
        .containsExactlyElementsOf(frontendOrigins);
    assertThat(configuration.getAllowCredentials()).isTrue();
  }
}
