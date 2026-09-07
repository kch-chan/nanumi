package com.nanumi.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 스프링이 뜨는지만 확인하는 테스트임
//
// 별것 아닌 것 같아도, 설정값이 빠졌거나 빈이 안 물리면 여기서 바로 걸림
// 특히 JWT 키가 없으면 JwtTokenProvider 의 @PostConstruct 에서 터지므로,
// 클론한 사람이 바로 실행하지 못하는 상태를 CI 가 잡아 줌
@SpringBootTest
@DisplayName("애플리케이션 기동")
class ApiApplicationTests {

  @Test
  @DisplayName("스프링 컨텍스트가 뜸")
  void contextLoads() {}
}
