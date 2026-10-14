package com.nanumi.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;

// 모듈을 실제 JsonMapper 에 끼워서, JSON 을 읽을 때 문자열이 다듬어지는지 확인함
@DisplayName("Jackson 설정")
class JacksonConfigTest {

  private record Form(String nickname, String password) {}

  private JsonMapper mapperWithModule() {
    JacksonModule module = new JacksonConfig().sanitizingStringModule();
    return JsonMapper.builder().addModule(module).build();
  }

  @Test
  @DisplayName("문자열 다듬기 모듈을 만들어 줌")
  void 모듈_생성() {
    assertThat(new JacksonConfig().sanitizingStringModule()).isNotNull();
  }

  @Test
  @DisplayName("JSON 을 읽을 때 앞뒤 공백이 떨어짐")
  void 공백_제거() {
    Form form = mapperWithModule().readValue("{\"nickname\":\"  나눔이  \"}", Form.class);

    assertThat(form.nickname()).isEqualTo("나눔이");
  }

  // 전각 문자로 중복 닉네임 검사를 피해 가는 걸 막음
  @Test
  @DisplayName("JSON 을 읽을 때 전각 문자가 반각으로 맞춰짐")
  void 전각_정규화() {
    Form form = mapperWithModule().readValue("{\"nickname\":\"ａｄｍｉｎ\"}", Form.class);

    assertThat(form.nickname()).isEqualTo("admin");
  }

  @Test
  @DisplayName("JSON 을 읽을 때 보이지 않는 문자가 지워짐")
  void 보이지_않는_문자() {
    Form form = mapperWithModule().readValue("{\"nickname\":\"나눔\\u200B이\"}", Form.class);

    assertThat(form.nickname()).isEqualTo("나눔이");
  }

  // 비밀번호는 공백까지 회원이 정한 값이라 건드리면 안 됨
  @Test
  @DisplayName("비밀번호 칸은 앞뒤 공백이 남음")
  void 비밀번호는_그대로() {
    Form form = mapperWithModule().readValue("{\"password\":\"  Ab3!efgh  \"}", Form.class);

    assertThat(form.password()).isEqualTo("  Ab3!efgh  ");
  }

  // 모듈이 빠지면 아무것도 안 다듬어진다는 것을 확인해서, 위 결과가 모듈 덕분임을 분명히 함
  @Test
  @DisplayName("모듈이 없으면 다듬지 않음")
  void 모듈이_없으면() {
    Form form = JsonMapper.builder().build().readValue("{\"nickname\":\"  나눔이  \"}", Form.class);

    assertThat(form.nickname()).isEqualTo("  나눔이  ");
  }
}
