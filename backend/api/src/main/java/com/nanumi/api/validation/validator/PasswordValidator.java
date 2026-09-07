package com.nanumi.api.validation.validator;

import com.nanumi.api.validation.annotation.ValidPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

// 비밀번호 형식 검사임
//
// 한글이나 공백을 허용하면 회원 쪽 입력기(IME)나 자동완성에 따라 글자가 달라져서
// 가입할 때와 로그인할 때의 값이 어긋날 수 있으므로 출력 가능한 ASCII 로만 제한함
// 걸린 규칙마다 메시지를 다르게 돌려줌
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

  private static final int MIN_LENGTH = 8;
  private static final int MAX_LENGTH = 20;

  // 공백과 제어문자를 뺀 출력 가능한 ASCII 임
  private static final Pattern ASCII_PATTERN = Pattern.compile("^[\\x21-\\x7E]+$");

  private static final Pattern LETTER_PATTERN = Pattern.compile("[A-Za-z]");
  private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
  private static final Pattern SPECIAL_PATTERN = Pattern.compile("[^A-Za-z0-9]");

  @Override
  public boolean isValid(String password, ConstraintValidatorContext context) {
    if (password == null || password.isEmpty()) {
      return reject(context, "비밀번호를 입력해 주세요.");
    }

    // 공백은 ASCII 라서 아래 ASCII 검사에 걸리지 않으므로 따로 봄
    if (containsWhitespace(password)) {
      return reject(context, "비밀번호에는 공백을 포함할 수 없습니다.");
    }

    if (!ASCII_PATTERN.matcher(password).matches()) {
      return reject(context, "비밀번호에는 영문, 숫자, 특수문자만 사용할 수 있습니다.");
    }

    if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
      return reject(context, "비밀번호는 " + MIN_LENGTH + "~" + MAX_LENGTH + "자여야 합니다.");
    }

    if (!LETTER_PATTERN.matcher(password).find()
        || !DIGIT_PATTERN.matcher(password).find()
        || !SPECIAL_PATTERN.matcher(password).find()) {
      return reject(context, "비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.");
    }

    return true;
  }

  private boolean containsWhitespace(String password) {
    for (int i = 0; i < password.length(); i++) {
      if (Character.isWhitespace(password.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private boolean reject(ConstraintValidatorContext context, String message) {
    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    return false;
  }
}
