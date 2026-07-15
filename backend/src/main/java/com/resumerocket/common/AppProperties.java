package com.resumerocket.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

  private final Jwt jwt = new Jwt();
  private final Cors cors = new Cors();
  private final Export export = new Export();

  public Jwt getJwt() {
    return jwt;
  }

  public Cors getCors() {
    return cors;
  }

  public Export getExport() {
    return export;
  }

  public static class Jwt {
    private String secret;
    private long expirationMinutes = 60;

    public String getSecret() {
      return secret;
    }

    public void setSecret(String secret) {
      this.secret = secret;
    }

    public long getExpirationMinutes() {
      return expirationMinutes;
    }

    public void setExpirationMinutes(long expirationMinutes) {
      this.expirationMinutes = expirationMinutes;
    }
  }

  public static class Cors {
    private String allowedOrigins = "http://localhost:5173";

    public String getAllowedOrigins() {
      return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
      this.allowedOrigins = allowedOrigins;
    }
  }

  public static class Export {
    private String storageDir = "./data/exports";

    public String getStorageDir() {
      return storageDir;
    }

    public void setStorageDir(String storageDir) {
      this.storageDir = storageDir;
    }
  }
}
