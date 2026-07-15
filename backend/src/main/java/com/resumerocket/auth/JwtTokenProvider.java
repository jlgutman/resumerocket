package com.resumerocket.auth;

import com.resumerocket.common.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private static final String CLAIM_USER_ID = "uid";

  private final AppProperties appProperties;
  private final SecretKey signingKey;

  public JwtTokenProvider(AppProperties appProperties) {
    this.appProperties = appProperties;
    this.signingKey =
        Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateToken(UserAccount user) {
    Instant now = Instant.now();
    Instant expiry = now.plus(appProperties.getJwt().getExpirationMinutes(), ChronoUnit.MINUTES);
    return Jwts.builder()
        .subject(user.getEmail())
        .claim(CLAIM_USER_ID, user.getId())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(signingKey)
        .compact();
  }

  public Claims parseClaims(String token) throws JwtException {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }

  public String getEmail(Claims claims) {
    return claims.getSubject();
  }

  public Long getUserId(Claims claims) {
    return claims.get(CLAIM_USER_ID, Long.class);
  }
}
