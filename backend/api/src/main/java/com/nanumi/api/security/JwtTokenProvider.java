package com.nanumi.api.security;

import com.nanumi.api.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

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
        .signWith(privateKey, SignatureAlgorithm.RS256)
        .compact();
  }

  private Claims parseClaims(String token) {
    return Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token).getBody();
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
}
