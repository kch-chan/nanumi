package com.nanumi.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanumi.api.validation.annotation.SafeText;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// 값을 다듬어 저장하지 않고 그냥 거절하는 쪽을 택했음
// 다듬어 저장하면 회원이 적은 것과 저장된 것이 달라지고, 거르는 규칙에 구멍이 나면 그대로 새어 나감
@DisplayName("자유 입력 XSS 검사")
class SafeTextValidatorTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  private record Form(@SafeText String text) {}

  private Set<String> messagesOf(String text) {
    return validator.validate(new Form(text)).stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toSet());
  }

  @ParameterizedTest
  @ValueSource(strings = {"행복아파트", "101동 1502호", "Nanumi Apt.", "가격 100원 & 덤"})
  @DisplayName("평범한 글은 통과함")
  void 평범한_글(String text) {
    assertThat(messagesOf(text)).isEmpty();
  }

  // 비어 있는지는 @NotBlank 가 봄. 여기서는 통과시켜야 메시지가 겹치지 않음
  @Test
  @DisplayName("null 은 통과시킴")
  void null_은_통과() {
    assertThat(messagesOf(null)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"<script>", "a > b", "행복<아파트", "닫는>괄호"})
  @DisplayName("꺾쇠가 있으면 막음")
  void 태그(String text) {
    assertThat(messagesOf(text)).containsExactly("HTML 태그는 사용할 수 없습니다.");
  }

  // 문자 참조를 허용하면 화면단에서 태그로 되살아날 수 있음
  @ParameterizedTest
  @ValueSource(strings = {"&lt;script&gt;", "&#60;", "&#x3c;", "&amp;"})
  @DisplayName("HTML 문자 참조가 있으면 막음")
  void 문자_참조(String text) {
    assertThat(messagesOf(text)).containsExactly("HTML 문자 참조는 사용할 수 없습니다.");
  }

  // "JaVaScRiPt :" 처럼 끼워 넣어 숨기는 것도 걸러야 함
  @ParameterizedTest
  @ValueSource(
      strings = {
        "javascript:alert(1)",
        "JaVaScRiPt : alert(1)",
        "vbscript:x",
        "data:text/html",
        "file:///etc",
        "blob:abc"
      })
  @DisplayName("스크립트가 되는 주소는 막음")
  void 스크립트_주소(String text) {
    assertThat(messagesOf(text)).containsExactly("스크립트 주소는 사용할 수 없습니다.");
  }

  @Test
  @DisplayName("보이지 않는 문자가 섞이면 막음")
  void 보이지_않는_문자() {
    // 제로 폭 공백(U+200B)
    assertThat(messagesOf("행복" + (char) 0x200B + "아파트")).containsExactly("보이지 않는 문자는 사용할 수 없습니다.");
    // 소프트 하이픈(U+00AD)
    assertThat(messagesOf("행복" + (char) 0x00AD + "아파트")).containsExactly("보이지 않는 문자는 사용할 수 없습니다.");
    // 글자 방향을 뒤집는 문자(U+202E)
    assertThat(messagesOf("행복" + (char) 0x202E + "아파트")).containsExactly("보이지 않는 문자는 사용할 수 없습니다.");
    // 파일 앞에 붙는 표식(U+FEFF)
    assertThat(messagesOf((char) 0xFEFF + "행복아파트")).containsExactly("보이지 않는 문자는 사용할 수 없습니다.");
  }

  @Test
  @DisplayName("어느 규칙에 걸렸는지 메시지를 하나만 돌려줌")
  void 메시지는_하나() {
    assertThat(messagesOf("<script>&lt;javascript:")).hasSize(1);
  }
}
