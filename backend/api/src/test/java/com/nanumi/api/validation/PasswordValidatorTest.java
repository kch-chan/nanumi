package com.nanumi.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanumi.api.validation.annotation.ValidPassword;
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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("비밀번호 형식 검사")
class PasswordValidatorTest {

  private static final String COMPOSITION_MESSAGE = "비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.";
  private static final String LENGTH_MESSAGE = "비밀번호는 8~64자여야 합니다.";

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

  private record PasswordHolder(@ValidPassword String password) {}

  private List<String> messagesOf(String password) {
    return validator.validate(new PasswordHolder(password)).stream()
        .map(ConstraintViolation::getMessage)
        .toList();
  }

  @ParameterizedTest
  @ValueSource(strings = {"nanumi1234!", "Ab3!efgh"})
  @DisplayName("규칙을 다 지킨 비밀번호는 통과함")
  void 올바른_비밀번호는_통과함(String password) {
    assertThat(messagesOf(password)).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("비어 있으면 입력하라고 알려 줌")
  void 비어_있으면_알려_줌(String password) {
    assertThat(messagesOf(password)).containsExactly("비밀번호를 입력해 주세요.");
  }

  @Test
  @DisplayName("공백이 들어가면 공백 때문이라고 알려 줌")
  void 공백이_있으면_알려_줌() {
    assertThat(messagesOf("nanumi 1234!")).containsExactly("비밀번호에는 공백을 포함할 수 없습니다.");
  }

  @Test
  @DisplayName("한글이 들어가면 쓸 수 있는 문자를 알려 줌")
  void 한글이_들어가면_알려_줌() {
    assertThat(messagesOf("나눔이1234!")).containsExactly("비밀번호에는 영문, 숫자, 특수문자만 사용할 수 있습니다.");
  }

  @Test
  @DisplayName("8자보다 짧으면 길이를 알려 줌")
  void 너무_짧으면_알려_줌() {
    assertThat(messagesOf("Ab3!efg")).containsExactly(LENGTH_MESSAGE);
  }

  @Test
  @DisplayName("64자를 넘으면 길이를 알려 줌")
  void 너무_길면_알려_줌() {
    assertThat(messagesOf("Ab3!" + "a".repeat(61))).containsExactly(LENGTH_MESSAGE);
  }

  // 20자로 묶어 두면 긴 비밀번호를 쓰려는 사람을 막게 됨
  @Test
  @DisplayName("20자를 넘어도 64자까지는 통과함")
  void 긴_비밀번호도_통과함() {
    assertThat(messagesOf("Ab3!" + "a".repeat(60))).isEmpty();
  }

  @Test
  @DisplayName("영문·숫자·특수문자 중 하나라도 빠지면 알려 줌")
  void 구성이_부족하면_알려_줌() {
    assertThat(messagesOf("abcdefgh!")).containsExactly(COMPOSITION_MESSAGE);
    assertThat(messagesOf("12345678!")).containsExactly(COMPOSITION_MESSAGE);
    assertThat(messagesOf("abcd1234")).containsExactly(COMPOSITION_MESSAGE);
  }
}
