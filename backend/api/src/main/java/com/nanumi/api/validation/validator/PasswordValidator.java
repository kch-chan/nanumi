package com.nanumi.api.validation.validator;

import java.util.regex.Pattern;

import com.nanumi.api.validation.annotation.ValidPassword;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String>{
  private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&]).{8,20}$");

  @Override
  public boolean isValid(String password, ConstraintValidatorContext context) {
      if (password == null) {
        return false;
      }

      return PASSWORD_PATTERN
        .matcher(password)
        .matches();
    }
}
