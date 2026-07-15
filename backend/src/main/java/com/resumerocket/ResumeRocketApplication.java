package com.resumerocket;

import com.resumerocket.common.FlywayMigrationInitializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// UserDetailsServiceAutoConfiguration is excluded: AuthService checks credentials directly
// against UserAccountRepository, so no UserDetailsService/AuthenticationManager is needed, and
// leaving it enabled just generates a throwaway in-memory user + random password at every boot.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class ResumeRocketApplication {

  public static void main(String[] args) {
    new SpringApplicationBuilder(ResumeRocketApplication.class)
        .initializers(new FlywayMigrationInitializer())
        .run(args);
  }
}
