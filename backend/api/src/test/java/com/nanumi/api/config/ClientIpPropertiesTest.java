package com.nanumi.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("접속자 IP 설정값")
class ClientIpPropertiesTest {

  // 기본값이 0 이어야 하는 이유
  //
  // 0 은 "프록시가 없으니 X-Forwarded-For 를 보지 말고 실제 연결 주소를 써라" 는 뜻임.
  // 만약 기본값이 1 이상이면, 프록시가 없는 환경(로컬·단독 배포)에서 요청자가 헤더를
  // 직접 지어내 보내는 것만으로 시도 제한(로그인 잠금·가입 제한)을 그대로 통과할 수 있음.
  // 설정을 빠뜨렸을 때 "덜 믿는 쪽" 으로 떨어지는 것이 안전한 기본값임
  @Test
  @DisplayName("신뢰 프록시 수의 기본값은 0 임")
  void 기본값() {
    assertThat(new ClientIpProperties().getTrustedProxyCount()).isZero();
  }

  // Render 는 프록시 한 대 뒤에 앱을 둠. CDN 을 하나 더 얹으면 2 가 됨
  @Test
  @DisplayName("설정으로 값을 덮어쓸 수 있음")
  void 값_덮어쓰기() {
    ClientIpProperties properties = new ClientIpProperties();

    properties.setTrustedProxyCount(1);

    assertThat(properties.getTrustedProxyCount()).isEqualTo(1);
  }
}
