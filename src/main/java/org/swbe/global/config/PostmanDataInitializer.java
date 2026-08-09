package org.swbe.global.config;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Component
@Profile("postman")
@RequiredArgsConstructor
public class PostmanDataInitializer implements ApplicationRunner {

  private final DataSource dataSource;

  @Override
  public void run(ApplicationArguments args) {
    ResourceDatabasePopulator populator =
        new ResourceDatabasePopulator(
            new ClassPathResource("data-postman.sql")
        );

    populator.execute(dataSource);
  }
}
