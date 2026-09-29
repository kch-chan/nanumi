package com.nanumi.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanumi.api.validation.annotation.ValidPassword;
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

// Bean Validation 만 띄워서 확인함. 스프링도 DB 도 필요 없음
@DisplayName("비밀번호 형식 검사")
class PasswordValidatorTest {

  private static final String LENGTH_MESSAGE = "비밀번호는 8~20자여야 합니다.";
  private static final String COMPOSITION_MESSAGE = "비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.";

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

  private record Form(@ValidPassword String password) {}

  private Set<String> messagesOf(String password) {
    return validator.validate(new Form(password)).stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toSet());
  }

  @Test
  @DisplayName("규칙을 다 지키면 통과함")
  void 올바른_비밀번호는_통과함() {
    assertThat(messagesOf("Ab3!efgh")).isEmpty();
  }

  @Test
  @DisplayName("8자보다 짧으면 길이를 알려 줌")
  void 너무_짧으면_알려_줌() {
    assertThat(messagesOf("Ab3!efg")).containsExactly(LENGTH_MESSAGE);
  }

  // 상한을 넘겨도 걸러야 함. 상한이 없으면 긴 문자열로 PBKDF2 를 돌리게 할 수 있음
  @Test
  @DisplayName("20자를 넘으면 길이를 알려 줌")
  void 너무_길면_알려_줌() {
    assertThat(messagesOf("Ab3!" + "a".repeat(17))).containsExactly(LENGTH_MESSAGE);
  }

  @Test
  @DisplayName("딱 20자는 통과함")
  void 상한_길이는_통과함() {
    assertThat(messagesOf("Ab3!" + "a".repeat(16))).isEmpty();
  }

  @Test
  @DisplayName("영문·숫자·특수문자 중 하나라도 빠지면 알려 줌")
  void 구성이_부족하면_알려_줌() {
    assertThat(messagesOf("abcdefgh!")).containsExactly(COMPOSITION_MESSAGE);
  }

  // 입력기(IME)나 자동완성에 따라 글자가 달라질 수 있어서 한글·공백은 막음
  @Test
  @DisplayName("한글이 섞이면 알려 줌")
  void 한글은_막음() {
    assertThat(messagesOf("나눔이1234!")).containsExactly("비밀번호에는 영문, 숫자, 특수문자만 사용할 수 있습니다.");
  }
}
