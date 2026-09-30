package com.nanumi.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanumi.api.validation.annotation.ValidEmail;
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

@DisplayName("이메일 형식 검사")
class EmailValidatorTest {

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

  private record Form(@ValidEmail String email) {}

  private Set<String> messagesOf(String email) {
    return validator.validate(new Form(email)).stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toSet());
  }

  @ParameterizedTest
  @ValueSource(strings = {"ab@c.de", "nanumi@example.com", "a.b+tag_1@sub.example.co.kr"})
  @DisplayName("올바른 주소는 통과함")
  void 올바른_주소(String email) {
    assertThat(messagesOf(email)).isEmpty();
  }

  @Test
  @DisplayName("비어 있으면 알려 줌")
  void 빈_값() {
    assertThat(messagesOf("")).containsExactly("이메일을 입력해 주세요.");
    assertThat(messagesOf(null)).containsExactly("이메일을 입력해 주세요.");
  }

  // 공백은 ASCII 라서 아래 ASCII 검사에 안 걸림. 따로 봐야 함
  @Test
  @DisplayName("공백이 섞이면 알려 줌")
  void 공백() {
    assertThat(messagesOf("na numi@example.com")).containsExactly("이메일에는 공백을 포함할 수 없습니다.");
  }

  @Test
  @DisplayName("골뱅이가 하나가 아니면 알려 줌")
  void 골뱅이_개수() {
    assertThat(messagesOf("a@b@example.com")).containsExactly("이메일에는 @를 하나만 포함해야 합니다.");
    assertThat(messagesOf("nanumi.example.com")).containsExactly("이메일에는 @를 하나만 포함해야 합니다.");
  }

  @Test
  @DisplayName("7자보다 짧으면 알려 줌")
  void 너무_짧음() {
    assertThat(messagesOf("a@b.cd")).containsExactly("이메일은 7자 이상이어야 합니다.");
  }

  // accounts.email 컬럼 길이(100)와 맞춰 둠. 어긋나면 저장할 때 잘림
  @Test
  @DisplayName("100자를 넘으면 알려 줌")
  void 너무_긺() {
    String email = "a".repeat(90) + "@example.com";
    assertThat(messagesOf(email)).containsExactly("이메일은 100자 이하여야 합니다.");
  }

  // 자바 기본 @Email 은 "가@나" 같은 것도 통과시킴
  @Test
  @DisplayName("한글이 섞이면 알려 줌")
  void 한글() {
    assertThat(messagesOf("나눔이@example.com")).containsExactly("이메일에는 영문, 숫자와 일부 기호만 사용할 수 있습니다.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"nanumi@example", "nanumi@.com", "@example.com", "nanumi@example.c"})
  @DisplayName("형식이 어긋나면 알려 줌")
  void 형식_불일치(String email) {
    assertThat(messagesOf(email)).containsExactly("올바른 이메일 형식이 아닙니다.");
  }

  // 어디가 틀렸는지 바로 알 수 있게 규칙마다 메시지를 하나만 돌려줌
  @Test
  @DisplayName("여러 규칙에 걸려도 메시지는 하나만 나옴")
  void 메시지는_하나() {
    assertThat(messagesOf("나 눔 이")).hasSize(1);
  }
}
