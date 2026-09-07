package com.nanumi.api.security;

import com.nanumi.api.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

// RS256 으로 토큰을 만들고 읽음
//
// 액세스 토큰과 리프레시 토큰은 typ 클레임으로 구분함
// 이게 없으면 만료 기간만 다르고 내용이 같아서, 리프레시 토큰을 Authorization 헤더에 넣어도
// 그대로 통과해 버림. 즉 리프레시 토큰이 14일짜리 액세스 토큰이 되어 버리므로 반드시 구분해야 함
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  // 토큰 종류를 적어 두는 클레임 이름임
  public static final String TOKEN_TYPE_CLAIM = "typ";

  private final JwtConfig jwtConfig;

  private final ResourceLoader resourceLoader = new DefaultResourceLoader();

  private PrivateKey privateKey;
  private PublicKey publicKey;

  @PostConstruct
  private void init() throws IOException, GeneralSecurityException {
    this.privateKey = readPrivateKey(jwtConfig.getPrivateKeyPath());
    this.publicKey = readPublicKey(jwtConfig.getPublicKeyPath());
  }

  public String createAccessToken(Long userId) {
    return createToken(userId, TokenType.ACCESS, jwtConfig.getAccessTokenExpiration());
  }

  public String createRefreshToken(Long userId) {
    return createToken(userId, TokenType.REFRESH, jwtConfig.getRefreshTokenExpiration());
  }

  public long getRefreshTokenExpiration() {
    return jwtConfig.getRefreshTokenExpiration();
  }

  // 서명·만료·종류를 한 번에 확인하고, 통과하면 회원 번호를 돌려줌
  // 예전에는 validateToken 과 getUserId 에서 토큰을 두 번 파싱했는데 한 번으로 합침
  public Optional<Long> resolveUserId(String token, TokenType expectedType) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }

    try {
      Claims claims = parseClaims(token);

      if (!expectedType.value().equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
        return Optional.empty();
      }

      return Optional.of(Long.valueOf(claims.getSubject()));
    } catch (JwtException | IllegalArgumentException e) {
      // 서명이 틀렸거나 만료됐거나 subject 가 숫자가 아님. 어느 쪽이든 못 믿는 토큰임
      return Optional.empty();
    }
  }

  private String createToken(Long userId, TokenType type, long expiration) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expiration);

    return Jwts.builder()
        .subject(String.valueOf(userId))
        // 토큰마다 다른 값을 넣어야 같은 순간에 발급해도 서로 다른 토큰이 나옴
        // 나중에 토큰 단위로 무효화할 때도 이 값을 씀
        .id(UUID.randomUUID().toString())
        .claim(TOKEN_TYPE_CLAIM, type.value())
        .issuedAt(now)
        .expiration(expiry)
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact();
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();
  }

  private PrivateKey readPrivateKey(String location) throws IOException, GeneralSecurityException {
    byte[] decoded = Base64.getDecoder().decode(readPem(location, "PRIVATE KEY"));
    return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
  }

  private PublicKey readPublicKey(String location) throws IOException, GeneralSecurityException {
    byte[] decoded = Base64.getDecoder().decode(readPem(location, "PUBLIC KEY"));
    return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
  }

  private String readPem(String location, String type) throws IOException {
    Resource resource = resourceLoader.getResource(location);
    try (InputStream inputStream = resource.getInputStream()) {
      String content = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
      return content
          .replace("-----BEGIN " + type + "-----", "")
          .replace("-----END " + type + "-----", "")
          .replaceAll("\\s", "");
    }
  }

  // 토큰 종류임. 값은 typ 클레임에 그대로 들어감
  public enum TokenType {
    ACCESS("access"),
    REFRESH("refresh");

    private final String value;

    TokenType(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }
}
