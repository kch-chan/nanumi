package com.nanumi.api.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.nanumi.api.validation.validator.PasswordValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = PasswordValidator.class)
public @interface ValidPassword {
  String message() default
    "비밀번호는 8~20자의 영문, 숫자, 특수문자를 포함해야 합니다.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
  
}
