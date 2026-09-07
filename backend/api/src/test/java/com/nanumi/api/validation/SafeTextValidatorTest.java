package com.nanumi.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanumi.api.validation.annotation.SafeText;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("자유 입력 XSS 차단")
class SafeTextValidatorTest {

  private static final String ZERO_WIDTH_SPACE = String.valueOf((char) 0x200B);

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void closeValidator() {
    factory.close();
  }

  private record TextHolder(@SafeText String text) {}

  private List<String> messagesOf(String text) {
    return validator.validate(new TextHolder(text)).stream()
        .map(ConstraintViolation::getMessage)
        .toList();
  }

  @ParameterizedTest
  @ValueSource(strings = {"행복아파트 101동", "냉장고 나눔합니다 :)"})
  @DisplayName("평범한 글은 통과함")
  void 평범한_글은_통과함(String text) {
    assertThat(messagesOf(text)).isEmpty();
  }

  @Test
  @DisplayName("비어 있는지는 @NotBlank 가 보므로 null 은 통과시킴")
  void null_은_통과함() {
    assertThat(messagesOf(null)).isEmpty();
  }

  @Test
  @DisplayName("꺾쇠가 들어가면 태그로 보고 막음")
  void 태그가_있으면_막힘() {
    assertThat(messagesOf("<script>alert(1)</script>")).containsExactly("HTML 태그는 사용할 수 없습니다.");
    assertThat(messagesOf("2 > 1")).containsExactly("HTML 태그는 사용할 수 없습니다.");
  }

  @Test
  @DisplayName("문자 참조는 화면에서 태그로 되살아날 수 있어서 막음")
  void 문자_참조가_있으면_막힘() {
    assertThat(messagesOf("&lt;script&gt;")).containsExactly("HTML 문자 참조는 사용할 수 없습니다.");
    assertThat(messagesOf("&#60;")).containsExactly("HTML 문자 참조는 사용할 수 없습니다.");
  }

  @Test
  @DisplayName("스크립트 주소는 중간에 공백을 끼워 넣어도 막힘")
  void 스크립트_주소가_있으면_막힘() {
    assertThat(messagesOf("javascript:alert(1)")).containsExactly("스크립트 주소는 사용할 수 없습니다.");
    assertThat(messagesOf("JaVaScRiPt:alert(1)")).containsExactly("스크립트 주소는 사용할 수 없습니다.");
    assertThat(messagesOf("java script:alert(1)")).containsExactly("스크립트 주소는 사용할 수 없습니다.");
  }

  @Test
  @DisplayName("보이지 않는 문자를 끼워 넣으면 막힘")
  void 보이지_않는_문자가_있으면_막힘() {
    assertThat(messagesOf("나눔" + ZERO_WIDTH_SPACE + "이")).containsExactly("보이지 않는 문자는 사용할 수 없습니다.");
  }

  @Test
  @DisplayName("그냥 쓰는 앰퍼샌드는 막지 않음")
  void 앰퍼샌드는_통과함() {
    assertThat(messagesOf("우유 & 빵 나눔")).isEmpty();
  }
}
