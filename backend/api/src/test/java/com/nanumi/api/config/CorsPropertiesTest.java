package com.nanumi.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CORS 설정값")
class CorsPropertiesTest {

  // 목록을 비워 두면 어떤 출처도 허용하지 않음
  // 설정을 빠뜨렸을 때 아무 데서나 부를 수 있게 열리면 안 되므로 기본값이 빈 목록이어야 함
  @Test
  @DisplayName("허용 출처의 기본값은 빈 목록임")
  void 출처_기본값() {
    assertThat(new CorsProperties().getAllowedOrigins()).isEmpty();
  }

  @Test
  @DisplayName("기본 허용 메서드에 필요한 것들이 들어 있음")
  void 메서드_기본값() {
    assertThat(new CorsProperties().getAllowedMethods())
        .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
  }

  @Test
  @DisplayName("기본 허용 헤더는 Authorization 과 Content-Type 뿐임")
  void 헤더_기본값() {
    assertThat(new CorsProperties().getAllowedHeaders())
        .containsExactlyInAnyOrder("Authorization", "Content-Type");
  }

  // 토큰을 헤더로 실어 보내므로 쿠키를 주고받을 일이 없음
  // 켜 두면 출처를 * 로 열 수 없게 되고, 쿠키가 자동으로 실려 CSRF 위험이 생김
  @Test
  @DisplayName("자격 증명 전송은 꺼져 있음")
  void 자격_증명_기본값() {
    assertThat(new CorsProperties().isAllowCredentials()).isFalse();
  }

  @Test
  @DisplayName("사전 요청 캐시 시간의 기본값은 1시간임")
  void 캐시_기본값() {
    assertThat(new CorsProperties().getMaxAge()).isEqualTo(3600L);
  }

  @Test
  @DisplayName("설정으로 값을 덮어쓸 수 있음")
  void 값_덮어쓰기() {
    CorsProperties properties = new CorsProperties();

    properties.setAllowedOrigins(List.of("https://nanumi.com"));
    properties.setMaxAge(60L);

    assertThat(properties.getAllowedOrigins()).containsExactly("https://nanumi.com");
    assertThat(properties.getMaxAge()).isEqualTo(60L);
  }
}
