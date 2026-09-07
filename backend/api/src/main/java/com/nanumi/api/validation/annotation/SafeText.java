package com.nanumi.api.validation.annotation;

import com.nanumi.api.validation.validator.SafeTextValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 닉네임·아파트명·탈퇴 사유처럼 회원이 자유롭게 적는 값에 붙임
// 저장된 값이 나중에 화면에 그대로 그려질 수 있으므로 스크립트가 될 만한 입력을 아예 받지 않음
// 비어 있는지는 @NotBlank 가 보므로 여기서는 null 을 통과시킴
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = SafeTextValidator.class)
public @interface SafeText {
  String message() default "사용할 수 없는 문자가 포함되어 있습니다.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
