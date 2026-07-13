package com.nanumi.api.validation.validator;

import com.nanumi.api.validation.annotation.ValidNickname;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class NicknameValidator implements ConstraintValidator<ValidNickname, String> {
  private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]{2,10}$");

  @Override
  public boolean isValid(String nickname, ConstraintValidatorContext context) {
    if (nickname == null) {
      return false;
    }

    return NICKNAME_PATTERN.matcher(nickname).matches();
  }
}
