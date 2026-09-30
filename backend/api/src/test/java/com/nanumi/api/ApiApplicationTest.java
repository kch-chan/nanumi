package com.nanumi.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanumi.api.controller.AuthController;
import com.nanumi.api.security.JwtTokenProvider;
import com.nanumi.api.security.password.NanumiPasswordEncoder;
import com.nanumi.api.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.cors.CorsConfigurationSource;

// 스프링을 통째로 띄워 보는 테스트임
//
// 설정 파일 오타, 빈 이름 충돌, JWT 키를 못 읽는 상황처럼
// 단위 테스트로는 안 잡히고 서버를 띄워야 알 수 있는 문제를 여기서 잡음
// DB 는 인메모리를 씀 (src/test/resources/application-test.yml)
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("애플리케이션 기동")
class ApiApplicationTest {

  @Autowired private ApplicationContext context;

  @Test
  @DisplayName("스프링이 뜨고 주요 빈이 만들어짐")
  void 기동() {
    assertThat(context).isNotNull();
    assertThat(context.getBean(AuthController.class)).isNotNull();
    assertThat(context.getBean(AuthService.class)).isNotNull();
    assertThat(context.getBean("corsConfigurationSource", CorsConfigurationSource.class))
        .isNotNull();
  }

  // 키를 못 읽으면 JwtTokenProvider 의 초기화에서 바로 터짐
  @Test
  @DisplayName("JWT 키를 읽어서 토큰을 만들 수 있음")
  void 토큰_발급() {
    JwtTokenProvider provider = context.getBean(JwtTokenProvider.class);

    String token = provider.createAccessToken(1);

    assertThat(provider.resolveUserId(token, JwtTokenProvider.TokenType.ACCESS)).contains(1);
  }

  @Test
  @DisplayName("비밀번호 인코더가 설정값을 받아 동작함")
  void 인코더_동작() {
    NanumiPasswordEncoder encoder = context.getBean(NanumiPasswordEncoder.class);

    String encoded = encoder.encode("Ab3!efgh");

    assertThat(encoder.matches("Ab3!efgh", encoded)).isTrue();
  }
}
