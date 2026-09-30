package com.nanumi.api.validation.validator;

import com.nanumi.api.validation.annotation.ValidEmail;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

// 이메일 형식 검사임
//
// 자바 기본 @Email 은 "가@나" 같은 것도 통과시켜서 직접 확인함
// 걸린 규칙마다 메시지를 다르게 돌려줘서 어디가 틀렸는지 바로 알 수 있게 함
public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

  // "ab@c.de" 가 딱 7자임. 이보다 짧으면 정상적인 주소가 나올 수 없음
  private static final int MIN_LENGTH = 7;

  // accounts.email 컬럼 길이와 맞춤
  private static final int MAX_LENGTH = 100;

  // 공백과 제어문자를 뺀 출력 가능한 ASCII 임. 한글·일본어 등은 여기서 걸림
  private static final Pattern ASCII_PATTERN = Pattern.compile("^[\\x21-\\x7E]+$");

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

  @Override
  public boolean isValid(String email, ConstraintValidatorContext context) {
    if (email == null || email.isBlank()) {
      return reject(context, "이메일을 입력해 주세요.");
    }

    // 공백은 ASCII 라서 아래 ASCII 검사에 걸리지 않으므로 따로 봄
    if (containsWhitespace(email)) {
      return reject(context, "이메일에는 공백을 포함할 수 없습니다.");
    }

    if (countAtSign(email) != 1) {
      return reject(context, "이메일에는 @를 하나만 포함해야 합니다.");
    }

    if (email.length() < MIN_LENGTH) {
      return reject(context, "이메일은 " + MIN_LENGTH + "자 이상이어야 합니다.");
    }

    if (email.length() > MAX_LENGTH) {
      return reject(context, "이메일은 " + MAX_LENGTH + "자 이하여야 합니다.");
    }

    if (!ASCII_PATTERN.matcher(email).matches()) {
      return reject(context, "이메일에는 영문, 숫자와 일부 기호만 사용할 수 있습니다.");
    }

    if (!EMAIL_PATTERN.matcher(email).matches()) {
      return reject(context, "올바른 이메일 형식이 아닙니다.");
    }

    return true;
  }

  private boolean containsWhitespace(String email) {
    for (int i = 0; i < email.length(); i++) {
      if (Character.isWhitespace(email.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private int countAtSign(String email) {
    int count = 0;
    for (int i = 0; i < email.length(); i++) {
      if (email.charAt(i) == '@') {
        count++;
      }
    }
    return count;
  }

  // 기본 메시지를 끄고 규칙별 메시지로 바꿔 담음
  private boolean reject(ConstraintValidatorContext context, String message) {
    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    return false;
  }
}
