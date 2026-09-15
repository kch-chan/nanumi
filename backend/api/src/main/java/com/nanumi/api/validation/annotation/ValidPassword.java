package com.nanumi.api.validation.annotation;

import com.nanumi.api.validation.validator.PasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 비밀번호 형식 검사임
// 어느 규칙에 걸렸는지 PasswordValidator 가 그때그때 다른 메시지로 알려 주므로,
// 아래 message 는 검증기가 메시지를 못 정했을 때만 쓰임
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = PasswordValidator.class)
public @interface ValidPassword {
  String message() default "비밀번호는 8~20자의 영문, 숫자, 특수문자를 포함해야 합니다.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
