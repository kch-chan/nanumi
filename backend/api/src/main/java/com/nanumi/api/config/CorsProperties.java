package com.nanumi.api.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 브라우저에서 이 API 를 직접 부를 수 있는 출처 목록임
// 프런트가 다른 포트(개발)나 다른 도메인(운영)에서 뜨므로 필요함
// 목록을 비워 두면 어떤 출처도 허용하지 않음
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "nanumi.security.cors")
public class CorsProperties {

  private List<String> allowedOrigins = new ArrayList<>();

  private List<String> allowedMethods =
      new ArrayList<>(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

  private List<String> allowedHeaders = new ArrayList<>(List.of("Authorization", "Content-Type"));

  // 토큰을 헤더로 실어 보내므로 쿠키를 주고받을 일이 없음
  private boolean allowCredentials = false;

  // 사전 요청(preflight) 결과를 브라우저가 캐시하는 시간(초)임
  private long maxAge = 3600;
}
