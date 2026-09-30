package com.nanumi.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanumi.api.validation.annotation.ValidNickname;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("닉네임 형식 검사")
class NicknameValidatorTest {

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

  private record Form(@ValidNickname String nickname) {}

  private boolean isValid(String nickname) {
    return validator.validate(new Form(nickname)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"나눔", "나눔이", "abc", "A1", "가나다라마바사아자차", "abcde12345"})
  @DisplayName("한글·영문·숫자 2~10자는 통과함")
  void 올바른_닉네임(String nickname) {
    assertThat(isValid(nickname)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"가", "a", "가나다라마바사아자차카", "abcde123456"})
  @DisplayName("2자 미만이거나 10자를 넘으면 막음")
  void 길이_위반(String nickname) {
    assertThat(isValid(nickname)).isFalse();
  }

  // 공백이나 기호를 허용하면 눈으로 구분되지 않는 닉네임이 생김
  @ParameterizedTest
  @ValueSource(strings = {"나 눔", "나눔!", "na-numi", "나눔@", "ㄱㄴ", "가나ㅏ"})
  @DisplayName("공백·기호·자모만 있는 닉네임은 막음")
  void 허용하지_않는_문자(String nickname) {
    assertThat(isValid(nickname)).isFalse();
  }

  @Test
  @DisplayName("null 과 빈 문자열은 막음")
  void 빈_값() {
    assertThat(isValid(null)).isFalse();
    assertThat(isValid("")).isFalse();
  }
}
