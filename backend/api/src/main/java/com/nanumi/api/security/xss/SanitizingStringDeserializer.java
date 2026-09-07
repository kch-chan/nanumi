package com.nanumi.api.security.xss;

import java.text.Normalizer;
import java.util.Locale;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

// 요청으로 들어오는 모든 문자열을 한 번 다듬어 주는 역직렬화기임
//
// 하는 일은 세 가지임
//  1. 유니코드 정규화(NFKC) - 겉보기는 같은데 코드가 다른 글자를 한 모양으로 맞춤
//     ("ａdmin" 같은 전각 문자로 중복 닉네임 검사를 피해 가는 걸 막음)
//  2. 제어문자와 보이지 않는 문자 제거 - 눈에 안 보이는 글자를 끼워 넣어 검사를 피해 가는 걸 막음
//  3. 앞뒤 공백 제거 - 다만 비밀번호는 공백도 글자로 쳐야 하므로 건드리지 않음
//
// 검증(@SafeText 등)보다 먼저 도는 자리라, 여기서 다듬고 나면 검증기는 깨끗한 값만 보게 됨
public class SanitizingStringDeserializer extends StdScalarDeserializer<String> {

  // 비밀번호는 앞뒤 공백까지 그대로 둬야 회원이 정한 값과 어긋나지 않음
  private static final String PASSWORD_MARKER = "password";

  private static final char SOFT_HYPHEN = 0x00AD;
  private static final char ZERO_WIDTH_START = 0x200B;
  private static final char ZERO_WIDTH_END = 0x200F;
  private static final char BIDI_START = 0x202A;
  private static final char BIDI_END = 0x202E;
  private static final char WORD_JOINER_START = 0x2060;
  private static final char WORD_JOINER_END = 0x2064;
  private static final char BYTE_ORDER_MARK = 0xFEFF;

  public SanitizingStringDeserializer() {
    super(String.class);
  }

  @Override
  public String deserialize(JsonParser parser, DeserializationContext context) {
    String value = parser.getValueAsString();
    if (value == null) {
      return null;
    }
    return sanitize(value, parser.currentName());
  }

  // 필드 이름에 password 가 들어가면 앞뒤 공백을 남김
  // 테스트에서 바로 부를 수 있도록 열어 둠
  public String sanitize(String value, String fieldName) {
    if (value == null) {
      return null;
    }

    String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
    String stripped = removeInvisible(normalized);

    return isPasswordField(fieldName) ? stripped : stripped.strip();
  }

  private boolean isPasswordField(String fieldName) {
    return fieldName != null && fieldName.toLowerCase(Locale.ROOT).contains(PASSWORD_MARKER);
  }

  // 줄바꿈과 탭도 함께 지움
  // 지금 받는 값 중에 여러 줄을 쓰는 항목이 없고, @SafeText 도 제어문자를 거절하므로 기준을 맞춰 둠
  private String removeInvisible(String value) {
    StringBuilder cleaned = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (isInvisible(c)) {
        continue;
      }
      cleaned.append(c);
    }
    return cleaned.toString();
  }

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
}
