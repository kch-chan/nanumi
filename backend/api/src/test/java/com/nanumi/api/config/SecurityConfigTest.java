package com.nanumi.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

// 필터 체인 전체는 스프링을 띄워야 볼 수 있으므로, 여기서는 CORS 정책 빈만 직접 만들어 확인함
@DisplayName("시큐리티 설정")
class SecurityConfigTest {

  private final SecurityConfig securityConfig = new SecurityConfig();

  private CorsConfiguration corsFor(String path, List<String> origins) {
    CorsProperties properties = new CorsProperties();
    properties.setAllowedOrigins(origins);

    CorsConfigurationSource source = securityConfig.corsConfigurationSource(properties);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(path);
    return source.getCorsConfiguration(request);
  }

  @Test
  @DisplayName("설정한 출처가 그대로 정책에 들어감")
  void 출처_반영() {
    CorsConfiguration configuration =
        corsFor("/api/auth/login", List.of("http://localhost:5173", "https://nanumi.com"));

    assertThat(configuration).isNotNull();
    // 정확히 일치하는 목록이 아니라 패턴으로 담김(와일드카드를 쓰기 위해)
    // 그래서 getAllowedOrigins() 가 아니라 getAllowedOriginPatterns() 를 봐야 함
    assertThat(configuration.getAllowedOriginPatterns())
        .containsExactly("http://localhost:5173", "https://nanumi.com");
    assertThat(configuration.getAllowedOrigins()).isNull();
  }

  // Vercel 은 커밋마다 프리뷰 주소를 새로 만듦
  // 와일드카드가 실제로 매칭되지 않으면 프리뷰에서 API 호출이 전부 막힘
  @Test
  @DisplayName("와일드카드 출처가 우리 프리뷰 주소만 허용함")
  void 와일드카드_출처() {
    CorsConfiguration configuration =
        corsFor(
            "/api/auth/login",
            List.of("https://nanumi-neon.vercel.app", "https://nanumi-neon-*.vercel.app"));

    assertThat(configuration).isNotNull();
    assertThat(configuration.checkOrigin("https://nanumi-neon.vercel.app"))
        .isEqualTo("https://nanumi-neon.vercel.app");
    assertThat(configuration.checkOrigin("https://nanumi-neon-git-feat-62-kch.vercel.app"))
        .isEqualTo("https://nanumi-neon-git-feat-62-kch.vercel.app");
    // 우리 프로젝트가 아닌 vercel.app 주소는 막혀야 함.
    // 전에 https://*.vercel.app 로 뒀더니 vercel.app 에 배포한 누구나 통과했음
    assertThat(configuration.checkOrigin("https://totally-unrelated.vercel.app")).isNull();
    // 관계없는 도메인은 여전히 막혀야 함
    assertThat(configuration.checkOrigin("https://evil.example.com")).isNull();
  }

  @Test
  @DisplayName("기본 메서드와 헤더가 정책에 들어감")
  void 메서드와_헤더_반영() {
    CorsConfiguration configuration = corsFor("/api/auth/login", List.of("http://localhost:5173"));

    assertThat(configuration.getAllowedMethods()).contains("GET", "POST", "OPTIONS");
    assertThat(configuration.getAllowedHeaders()).contains("Authorization", "Content-Type");
    assertThat(configuration.getAllowCredentials()).isFalse();
    assertThat(configuration.getMaxAge()).isEqualTo(3600L);
  }

  // /api 아래만 열어야 함. 다른 경로까지 열면 필요 없는 곳이 브라우저에 노출됨
  @Test
  @DisplayName("/api 바깥 경로에는 CORS 정책이 붙지 않음")
  void api_바깥은_없음() {
    assertThat(corsFor("/actuator/health", List.of("http://localhost:5173"))).isNull();
  }

  // 설정을 빠뜨렸을 때 아무 출처나 열리면 안 됨
  @Test
  @DisplayName("허용 출처를 비워 두면 아무 출처도 허용하지 않음")
  void 출처가_비면_빈_목록() {
    CorsConfiguration configuration = corsFor("/api/auth/login", List.of());

    assertThat(configuration.getAllowedOriginPatterns()).isEmpty();
    assertThat(configuration.checkOrigin("https://nanumi-neon.vercel.app")).isNull();
  }
}
