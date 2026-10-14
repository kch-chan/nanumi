package com.nanumi.api.config;

import jakarta.annotation.PostConstruct;
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

  // 출처 목록이 잘못 들어온 채로 뜨는 것을 막음
  //
  // 왜 기동을 막는가: 운영 프로필은 ${CORS_ALLOWED_ORIGINS} 를 환경 변수로 받는데,
  // @ConfigurationProperties 는 환경 변수가 없어도 예외를 던지지 않고
  // "${CORS_ALLOWED_ORIGINS}" 라는 글자를 그대로 값으로 넣음.
  // 그 상태로 뜨면 기동은 성공하는데 프런트의 모든 요청이 CORS 에 막혀서,
  // 원인이 서버가 아니라 브라우저 콘솔에만 나타남.
  // pepper 와 같은 이유로 여기서 막음 (NanumiPasswordProperties.valueCheck 참고)
  @PostConstruct
  void valueCheck() {
    if (allowedOrigins == null || allowedOrigins.isEmpty()) {
      throw new IllegalStateException(
          "nanumi.security.cors.allowed-origins 가 비어 있음. CORS_ALLOWED_ORIGINS 환경 변수를 넣어야 함");
    }
    for (String origin : allowedOrigins) {
      if (origin == null || origin.isBlank()) {
        throw new IllegalStateException(
            "nanumi.security.cors.allowed-origins 에 빈 항목이 들어옴: " + allowedOrigins);
      }
      if (origin.contains("${")) {
        throw new IllegalStateException(
            "nanumi.security.cors.allowed-origins 에 치환되지 않은 자리표시자가 들어옴: "
                + origin
                + " (CORS_ALLOWED_ORIGINS 환경 변수가 전달되지 않음)");
      }
    }
  }
}
