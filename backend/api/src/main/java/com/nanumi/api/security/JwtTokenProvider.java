package com.nanumi.api.security;

import com.nanumi.api.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  private final JwtConfig jwtConfig;

  private SecretKey secretKey;

  @PostConstruct
  private void init() {
    this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String createAccessToken(Long userId) {
    return createToken(userId, jwtConfig.getAccessTokenExpiration());
  }

  public String createRefreshToken(Long userId) {
    return createToken(userId, jwtConfig.getRefreshTokenExpiration());
  }

  public long getRefreshTokenExpiration() {
    return jwtConfig.getRefreshTokenExpiration();
  }

  public Long getUserId(String token) {
    return Long.valueOf(parseClaims(token).getSubject());
  }

  public boolean validateToken(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  private String createToken(Long userId, long expiration) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expiration);

    return Jwts.builder()
        .setSubject(String.valueOf(userId))
        .setIssuedAt(now)
        .setExpiration(expiry)
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  private Claims parseClaims(String token) {
    return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
  }
}
