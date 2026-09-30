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

  // 액세스 토큰과 리프레시 토큰 구분하는 표식

  public static final String TOKEN_TYPE_CLAIM = "typ";

  private final JwtConfig jwtConfig;

  private final ResourceLoader resourceLoader = new DefaultResourceLoader();

  private PrivateKey privateKey;
  private PublicKey publicKey;

  @PostConstruct
  private void init() throws IOException, GeneralSecurityException {
    this.privateKey =
        readPrivateKey(
            pemOf(jwtConfig.getPrivateKey(), jwtConfig.getPrivateKeyPath(), "PRIVATE KEY"));
    this.publicKey =
        readPublicKey(pemOf(jwtConfig.getPublicKey(), jwtConfig.getPublicKeyPath(), "PUBLIC KEY"));
  }

  // 설정에 PEM 본문이 들어 있으면 그걸 쓰고, 비어 있으면 경로에서 파일을 읽음
  // 비밀 저장소가 파일 대신 문자열만 줄 수 있는 환경(CI, 컨테이너)을 위한 것임
  private String pemOf(String content, String location, String type) throws IOException {
    if (content != null && !content.isBlank()) {
      return stripPem(content, type);
    }
    return readPem(location, type);
  }

  public String createAccessToken(Integer userId) {
    return createToken(userId, TokenType.ACCESS, jwtConfig.getAccessTokenExpiration());
  }

  public String createRefreshToken(Integer userId) {
    return createToken(userId, TokenType.REFRESH, jwtConfig.getRefreshTokenExpiration());
  }

  public long getRefreshTokenExpiration() {
    return jwtConfig.getRefreshTokenExpiration();
  }

  // 서명·만료·종류를 한 번에 확인하고, 통과하면 회원 번호를 돌려줌
  // 예전에는 validateToken 과 getUserId 에서 토큰을 두 번 파싱했는데 한 번으로 합침
  public Optional<Integer> resolveUserId(String token, TokenType expectedType) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }

    try {
      Claims claims = parseClaims(token);

      // 토큰과 검증기에서 기대하는 토큰 종류가 다를 때
      if (!expectedType.value().equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
        return Optional.empty();
      }

      return Optional.of(Integer.valueOf(claims.getSubject()));
    } catch (JwtException | IllegalArgumentException e) {
      // 서명이 틀렸거나 만료됐거나 subject 가 숫자가 아님. 어느 쪽이든 못 믿는 토큰임
      return Optional.empty();
    }
  }

  private String createToken(Integer userId, TokenType type, long expiration) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expiration);

    return Jwts.builder()
        // 누구 것인가
        .subject(String.valueOf(userId))
        // 같은 순간에 발급해도 다른 토큰을 만들어 줌
        .id(UUID.randomUUID().toString())
        // 커스텀 클레임 생성. "typ": "access" 또는 "refresh"
        .claim(TOKEN_TYPE_CLAIM, type.value())
        .issuedAt(now)
        .expiration(expiry)
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact();
  }

  // publickey로 검증
  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();
  }

  private PrivateKey readPrivateKey(String base64) throws GeneralSecurityException {
    // Base64를 바이트로 되돌림
    byte[] decoded = Base64.getDecoder().decode(base64);
    // 개인키를 담는 표준 형식을 이용. RSA 개인키를 Java에서 실제 PrivateKey 객체로 변환하는 과정
    return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
  }

  private PublicKey readPublicKey(String base64) throws GeneralSecurityException {
    byte[] decoded = Base64.getDecoder().decode(base64);
    return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
  }

  private String readPem(String location, String type) throws IOException {
    Resource resource = resourceLoader.getResource(location);
    try (InputStream inputStream = resource.getInputStream()) {
      return stripPem(StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8), type);
    }
  }

  // PEM 머리말과 줄바꿈을 걷어내고 Base64 본문만 남김
  private String stripPem(String content, String type) {
    return content
        .replace("-----BEGIN " + type + "-----", "")
        .replace("-----END " + type + "-----", "")
        .replaceAll("\\s", "");
  }

  // 오타 방지
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
