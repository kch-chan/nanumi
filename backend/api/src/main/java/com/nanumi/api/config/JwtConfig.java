package com.nanumi.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

  // 키 파일 경로. 로컬 개발에서 씀
  private String privateKeyPath;
  private String publicKeyPath;

  // PEM 본문을 직접 받는 자리. CI 나 운영에서 비밀 저장소가 파일 대신 문자열로 줄 때 씀
  // 비어 있지 않으면 위 경로보다 이쪽이 우선함
  private String privateKey;
  private String publicKey;
  private long accessTokenExpiration;
  private long refreshTokenExpiration;
}
