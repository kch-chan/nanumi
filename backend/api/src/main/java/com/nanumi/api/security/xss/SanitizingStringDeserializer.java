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

  // 비밀번호 필드 구분
  private static final String PASSWORD_MARKER = "password";

  private static final char SOFT_HYPHEN = 0x00AD; // 눈에 안 보이는 하이픈. 글자 수를 늘리거나 줄이는 데 쓰임
  private static final char ZERO_WIDTH_START = 0x200B; // 눈에 안 보이는 공백. 글자 수를 늘리거나 줄이는 데 쓰임
  private static final char ZERO_WIDTH_END = 0x200F;
  private static final char BIDI_START = 0x202A; // 글자 방향을 뒤집는 문자
  private static final char BIDI_END = 0x202E;
  private static final char WORD_JOINER_START = 0x2060; // 줄 바꿈을 방지하는 문자.
  private static final char WORD_JOINER_END = 0x2064;
  private static final char BYTE_ORDER_MARK = 0xFEFF; // 파일 앞에 붙는 보이지 않는 표식

  public SanitizingStringDeserializer() {
    super(String.class);
  }

  @Override
  public String deserialize(JsonParser parser, DeserializationContext context) {
    String value = parser.getValueAsString(); // JSON 값
    if (value == null) {
      return null;
    }
    return sanitize(value, parser.currentName());
  }

  public String sanitize(String value, String fieldName) {
    if (value == null) {
      return null;
    }

    // 전각 문자나 합성 문자를 한 모양으로 맞춤
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
    // 보이지 않는 문자 제거
    String stripped = removeInvisible(normalized);

    // 비밀번호는 앞뒤 공백까지 그대로 둬야 회원이 정한 값과 어긋나지 않음
    return isPasswordField(fieldName) ? stripped : stripped.strip();
  }

  private boolean isPasswordField(String fieldName) {
    // 특정 국가나 언어에 영향받지 않는 소문자로 바꿔서 검사
    return fieldName != null && fieldName.toLowerCase(Locale.ROOT).contains(PASSWORD_MARKER);
  }

  private String removeInvisible(String value) {
    // 문자열을 중간에 계속 추가하거나 수정하기 위해 담아둠
    StringBuilder cleaned = new StringBuilder(value.length());
    // 글자 하나씩 검사하면서 보이지 않는 글자는 건너뜀
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
    if (Character.isISOControl(c)) { // 제어문자. 줄바꿈, 탭, 백스페이스 등
      return true;
    }
    return c == SOFT_HYPHEN
        || (c >= ZERO_WIDTH_START && c <= ZERO_WIDTH_END)
        || (c >= BIDI_START && c <= BIDI_END)
        || (c >= WORD_JOINER_START && c <= WORD_JOINER_END)
        || c == BYTE_ORDER_MARK;
  }
}
