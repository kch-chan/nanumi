package com.nanumi.api.security.password;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("비밀번호 해시 설정값")
class NanumiPasswordPropertiesTest {

  // 반복 횟수는 해시 문자열에 같이 적히므로 나중에 올려도 기존 계정은 그대로 로그인됨
  @Test
  @DisplayName("반복 횟수 기본값은 21만 회임")
  void 반복_횟수_기본값() {
    assertThat(new NanumiPasswordProperties().getIterations()).isEqualTo(210_000);
  }

  // 설정을 빠뜨렸을 때 무엇으로 해싱됐는지 알 수 있도록 기본값은 빈 문자열임
  @Test
  @DisplayName("pepper 기본값은 빈 문자열임")
  void pepper_기본값() {
    assertThat(new NanumiPasswordProperties().getPepper()).isEmpty();
  }

  @Test
  @DisplayName("설정으로 값을 덮어쓸 수 있음")
  void 값_덮어쓰기() {
    NanumiPasswordProperties properties = new NanumiPasswordProperties();

    properties.setIterations(310_000);
    properties.setPepper("운영용-비밀값");

    assertThat(properties.getIterations()).isEqualTo(310_000);
    assertThat(properties.getPepper()).isEqualTo("운영용-비밀값");
  }
}
