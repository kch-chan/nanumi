package com.nanumi.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 모듈을 실제 JsonMapper 에 끼워서, JSON 을 읽을 때 문자열이 다듬어지는지 확인함
@DisplayName("Jackson 설정")
class JacksonConfigTest {

  @Test
  @DisplayName("문자열 다듬기 모듈을 만들어 줌")
  void 모듈_생성() {
    assertThat(new JacksonConfig().sanitizingStringModule()).isNotNull();
  }
}
