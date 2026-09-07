package com.nanumi.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanumi.api.validation.annotation.ValidEmail;
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

@DisplayName("이메일 형식 검사")
class EmailValidatorTest {

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

  private record EmailHolder(@ValidEmail String email) {}

  private List<String> messagesOf(String email) {
    return validator.validate(new EmailHolder(email)).stream()
        .map(ConstraintViolation::getMessage)
        .toList();
  }

  @ParameterizedTest
  @ValueSource(strings = {"ab@c.de", "nanumi@example.com", "user.name+tag@sub.example.co.kr"})
  @DisplayName("제대로 된 주소는 통과함")
  void 올바른_이메일은_통과함(String email) {
    assertThat(messagesOf(email)).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  @DisplayName("비어 있으면 입력하라고 알려 줌")
  void 비어_있으면_알려_줌(String email) {
    assertThat(messagesOf(email)).containsExactly("이메일을 입력해 주세요.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"na numi@example.com", "nanumi@exa mple.com"})
  @DisplayName("공백이 섞여 있으면 공백 때문이라고 알려 줌")
  void 공백이_있으면_알려_줌(String email) {
    assertThat(messagesOf(email)).containsExactly("이메일에는 공백을 포함할 수 없습니다.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"nanumiexample.com", "na@nu@mi.com", "nanumi@@example.com"})
  @DisplayName("@ 가 없거나 여러 개면 그걸 알려 줌")
  void 골뱅이_개수가_틀리면_알려_줌(String email) {
    assertThat(messagesOf(email)).containsExactly("이메일에는 @를 하나만 포함해야 합니다.");
  }

  @Test
  @DisplayName("7자보다 짧으면 길이를 알려 줌")
  void 너무_짧으면_알려_줌() {
    assertThat(messagesOf("a@b.cd")).containsExactly("이메일은 7자 이상이어야 합니다.");
  }

  @Test
  @DisplayName("100자를 넘으면 길이를 알려 줌")
  void 너무_길면_알려_줌() {
    String tooLong = "a".repeat(95) + "@ex.com";

    assertThat(messagesOf(tooLong)).containsExactly("이메일은 100자 이하여야 합니다.");
  }

  @Test
  @DisplayName("한글이 섞여 있으면 쓸 수 있는 문자를 알려 줌")
  void 한글이_들어가면_알려_줌() {
    assertThat(messagesOf("나눔@example.com")).containsExactly("이메일에는 영문, 숫자와 일부 기호만 사용할 수 있습니다.");
  }

  @Test
  @DisplayName("규칙은 다 지켰는데 모양이 아니면 형식을 알려 줌")
  void 형식이_어긋나면_알려_줌() {
    assertThat(messagesOf("nanumi@example")).containsExactly("올바른 이메일 형식이 아닙니다.");
  }
}
