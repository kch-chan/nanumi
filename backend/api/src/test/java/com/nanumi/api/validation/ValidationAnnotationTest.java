package com.nanumi.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanumi.api.validation.annotation.SafeText;
import com.nanumi.api.validation.annotation.ValidEmail;
import com.nanumi.api.validation.annotation.ValidNickname;
import com.nanumi.api.validation.annotation.ValidPassword;
import com.nanumi.api.validation.validator.EmailValidator;
import com.nanumi.api.validation.validator.NicknameValidator;
import com.nanumi.api.validation.validator.PasswordValidator;
import com.nanumi.api.validation.validator.SafeTextValidator;
import jakarta.validation.Constraint;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// 검증 애노테이션 4종의 "껍데기" 를 확인함
//
// 검증 규칙 자체는 각 Validator 테스트가 봄. 여기서 보는 것은 그 규칙이 실제로 연결되어 있는가임.
// @Constraint(validatedBy = ...) 를 지우거나 @Retention 을 CLASS 로 바꾸면 검증기가 조용히
// 돌지 않게 되는데, 그러면 어떤 값을 넣어도 통과해 버림. 컴파일도 되고 앱도 뜨므로
// 운영에 나가서야 알게 되는 종류의 사고라 여기서 막아 둠
@DisplayName("검증 애노테이션")
class ValidationAnnotationTest {

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      classes = {ValidEmail.class, ValidPassword.class, ValidNickname.class, SafeText.class})
  @DisplayName("런타임까지 남아 있어야 검증기가 돎")
  void 런타임_유지(Class<? extends Annotation> annotation) {
    Retention retention = annotation.getAnnotation(Retention.class);

    assertThat(retention).isNotNull();
    assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      classes = {ValidEmail.class, ValidPassword.class, ValidNickname.class, SafeText.class})
  @DisplayName("필드에만 붙일 수 있음")
  void 필드_전용(Class<? extends Annotation> annotation) {
    Target target = annotation.getAnnotation(Target.class);

    assertThat(target).isNotNull();
    assertThat(target.value()).containsExactly(ElementType.FIELD);
  }

  @Test
  @DisplayName("애노테이션마다 짝이 되는 검증기가 연결되어 있음")
  void 검증기_연결() {
    assertThat(validatedBy(ValidEmail.class)).containsExactly(EmailValidator.class);
    assertThat(validatedBy(ValidPassword.class)).containsExactly(PasswordValidator.class);
    assertThat(validatedBy(ValidNickname.class)).containsExactly(NicknameValidator.class);
    assertThat(validatedBy(SafeText.class)).containsExactly(SafeTextValidator.class);
  }

  // 검증기가 규칙별로 다른 문구를 직접 넣어 주므로 아래 기본값은 거의 쓰이지 않음.
  // 그래도 비어 있으면 안 됨 — 검증기가 문구를 못 정한 경우 그대로 응답에 나가기 때문
  @Test
  @DisplayName("기본 문구가 비어 있지 않음")
  void 기본_문구() {
    assertThat(defaultMessage(ValidEmail.class)).isEqualTo("올바른 이메일 형식이 아닙니다.");
    assertThat(defaultMessage(ValidPassword.class))
        .isEqualTo("비밀번호는 8~20자의 영문, 숫자, 특수문자를 포함해야 합니다.");
    assertThat(defaultMessage(ValidNickname.class)).isEqualTo("닉네임 형식이 맞지 않습니다.");
    assertThat(defaultMessage(SafeText.class)).isEqualTo("사용할 수 없는 문자가 포함되어 있습니다.");
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      classes = {ValidEmail.class, ValidPassword.class, ValidNickname.class, SafeText.class})
  @DisplayName("Bean Validation 규격에 맞는 메서드를 모두 갖춤")
  void 규격_메서드(Class<? extends Annotation> annotation) {
    assertThat(annotation.getDeclaredMethods())
        .extracting(java.lang.reflect.Method::getName)
        .contains("message", "groups", "payload");
  }

  private Class<?>[] validatedBy(Class<? extends Annotation> annotation) {
    Constraint constraint = annotation.getAnnotation(Constraint.class);

    assertThat(constraint).as("@Constraint 가 붙어 있어야 검증기가 돎").isNotNull();
    return constraint.validatedBy();
  }

  private String defaultMessage(Class<? extends Annotation> annotation) {
    try {
      return (String) annotation.getDeclaredMethod("message").getDefaultValue();
    } catch (NoSuchMethodException e) {
      throw new AssertionError("message() 가 없음: " + annotation.getSimpleName(), e);
    }
  }
}
