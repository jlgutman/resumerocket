package com.resumerocket.common;

import org.flywaydb.core.Flyway;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Runs Flyway migrations against {@code spring.datasource.*} before the context refreshes, so
 * JPA's {@code ddl-auto: validate} check always sees an up-to-date schema. Spring Boot 4's
 * built-in Flyway autoconfiguration does not reliably order itself ahead of eager JPA
 * repository bootstrapping in this project's dependency set, so migration is triggered
 * explicitly instead of relying on it.
 */
public class FlywayMigrationInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    Environment env = applicationContext.getEnvironment();
    String url = env.getProperty("spring.datasource.url");
    if (url == null) {
      return;
    }
    Flyway.configure(applicationContext.getClassLoader())
        .dataSource(
            url, env.getProperty("spring.datasource.username"), env.getProperty("spring.datasource.password"))
        .locations("classpath:db/migration")
        .load()
        .migrate();
  }
}
