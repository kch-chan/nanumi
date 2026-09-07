package com.nanumi.api.validation.validator;

import com.nanumi.api.validation.annotation.SafeText;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Locale;
import java.util.regex.Pattern;

// 자유 입력값에서 스크립트가 될 만한 것을 걸러 냄
//
// 값을 다듬어서 저장하지 않고 그냥 거절함
// 다듬어 저장하면 회원이 적은 것과 저장된 것이 달라지고, 거르는 규칙에 구멍이 생기면 그대로 새어 나가기 때문임
// 화면에 그릴 때 이스케이프하는 것과 별개로, 들어올 때 한 번 더 막는 셈임
public class SafeTextValidator implements ConstraintValidator<SafeText, String> {

  // &lt; &#60; &#x3c; 같은 문자 참조임. 이걸 허용하면 화면단에서 태그로 되살아날 수 있음
  private static final Pattern CHARACTER_REFERENCE =
      Pattern.compile("&(#[0-9]+|#[xX][0-9a-fA-F]+|[A-Za-z][A-Za-z0-9]{1,31});");

  // href 나 src 로 흘러 들어가면 스크립트가 되는 스킴임
  private static final String[] SCRIPT_SCHEMES = {
    "javascript:", "vbscript:", "data:", "file:", "blob:"
  };

  // 눈에 보이지 않아서 사람이 못 알아채는 문자들임
  private static final char SOFT_HYPHEN = 0x00AD;
  private static final char ZERO_WIDTH_START = 0x200B;
  private static final char ZERO_WIDTH_END = 0x200F;
  private static final char BIDI_START = 0x202A;
  private static final char BIDI_END = 0x202E;
  private static final char WORD_JOINER_START = 0x2060;
  private static final char WORD_JOINER_END = 0x2064;
  private static final char BYTE_ORDER_MARK = 0xFEFF;

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    // 비어 있는지는 @NotBlank 가 보므로 여기서는 통과시킴
    if (value == null) {
      return true;
    }

    if (containsTag(value)) {
      return reject(context, "HTML 태그는 사용할 수 없습니다.");
    }

    if (CHARACTER_REFERENCE.matcher(value).find()) {
      return reject(context, "HTML 문자 참조는 사용할 수 없습니다.");
    }

    if (containsScriptScheme(value)) {
      return reject(context, "스크립트 주소는 사용할 수 없습니다.");
    }

    if (containsInvisible(value)) {
      return reject(context, "보이지 않는 문자는 사용할 수 없습니다.");
    }

    return true;
  }

  private boolean containsTag(String value) {
    return value.indexOf('<') >= 0 || value.indexOf('>') >= 0;
  }

  // "java\nscript:" 나 "JaVaScRiPt :" 처럼 끼워 넣어 숨기는 걸 막으려고
  // 공백과 보이지 않는 문자를 먼저 걷어 내고 소문자로 맞춘 뒤에 확인함
  private boolean containsScriptScheme(String value) {
    StringBuilder squeezed = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (Character.isWhitespace(c) || isInvisible(c)) {
        continue;
      }
      squeezed.append(c);
    }

    String normalized = squeezed.toString().toLowerCase(Locale.ROOT);
    for (String scheme : SCRIPT_SCHEMES) {
      if (normalized.contains(scheme)) {
        return true;
      }
    }
    return false;
  }

  private boolean containsInvisible(String value) {
    for (int i = 0; i < value.length(); i++) {
      if (isInvisible(value.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  // 제어문자, 제로 폭 문자, 글자 방향을 뒤집는 문자를 모두 막음
  private boolean isInvisible(char c) {
    if (Character.isISOControl(c)) {
      return true;
    }
    return c == SOFT_HYPHEN
        || (c >= ZERO_WIDTH_START && c <= ZERO_WIDTH_END)
        || (c >= BIDI_START && c <= BIDI_END)
        || (c >= WORD_JOINER_START && c <= WORD_JOINER_END)
        || c == BYTE_ORDER_MARK;
  }

  private boolean reject(ConstraintValidatorContext context, String message) {
    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    return false;
  }
}
