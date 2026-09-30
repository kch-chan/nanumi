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

  // 키가 없을 때 임시로 한 쌍 만들어 쓸지 여부. 개발 프로필에서만 true
  // 저장소를 새로 받은 사람은 키 파일이 없으므로(.gitignore 대상) 그냥 기동되게 하려는 것임
  // 운영은 false 여야 함. 재시작마다 키가 바뀌면 전원이 로그아웃됨
  private boolean generateKeyIfMissing = false;
  private long accessTokenExpiration;
  private long refreshTokenExpiration;
}
