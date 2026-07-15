package com.resumerocket.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4's Jackson autoconfiguration defaults to the Jackson 3 ({@code tools.jackson})
 * engine and does not register a classic {@code com.fasterxml.jackson.databind.ObjectMapper}
 * bean, which this codebase uses directly for JSON-column (de)serialization and the LLM adapter.
 */
@Configuration
public class JacksonConfig {

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper().registerModule(new JavaTimeModule());
  }
}
