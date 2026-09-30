package com.nanumi.api.security.password;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("비밀번호 해시 설정값")
class NanumiPasswordPropertiesTest {

  // 반복 횟수는 해시 문자열에 같이 적히므로 나중에 올려도 기존 계정은 그대로 로그인됨
  @Test
  @DisplayName("반복 횟수 기본값은 21만 회임")
  void 반복_횟수_기본값() {
    assertThat(new NanumiPasswordProperties().getIterations()).isEqualTo(210_000);
  }

  // 설정을 빠뜨렸을 때 무엇으로 해싱됐는지 알 수 있도록 기본값은 빈 문자열임
  @Test
  @DisplayName("pepper 기본값은 빈 문자열임")
  void pepper_기본값() {
    assertThat(new NanumiPasswordProperties().getPepper()).isEmpty();
  }

  @Test
  @DisplayName("설정으로 값을 덮어쓸 수 있음")
  void 값_덮어쓰기() {
    NanumiPasswordProperties properties = new NanumiPasswordProperties();

    properties.setIterations(310_000);
    properties.setPepper("운영용-비밀값");

    assertThat(properties.getIterations()).isEqualTo(310_000);
    assertThat(properties.getPepper()).isEqualTo("운영용-비밀값");
  }

  // 여기부터는 기동 가드임
  // @ConfigurationProperties 는 환경 변수가 없을 때 예외를 던지지 않고 "${PASSWORD_PEPPER}" 라는
  // 글자를 그대로 넣어 버림. 그 상태로 만든 해시는 나중에 올바른 pepper 로 검증되지 않으므로
  // 비밀번호가 조용히 전부 망가짐. 그래서 기동 단계에서 끊어야 함
  @Test
  @DisplayName("pepper 가 비어 있으면 기동을 막음")
  void pepper_없으면_기동_실패() {
    NanumiPasswordProperties properties = new NanumiPasswordProperties();

    assertThatThrownBy(properties::valueCheck)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("PASSWORD_PEPPER");
  }

  @Test
  @DisplayName("치환되지 않은 자리표시자가 들어오면 기동을 막음")
  void 자리표시자_그대로면_기동_실패() {
    NanumiPasswordProperties properties = new NanumiPasswordProperties();
    properties.setPepper("${PASSWORD_PEPPER}");

    assertThatThrownBy(properties::valueCheck)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("자리표시자");
  }

  @Test
  @DisplayName("제대로 된 pepper 면 통과함")
  void 정상_pepper() {
    NanumiPasswordProperties properties = new NanumiPasswordProperties();
    properties.setPepper("f4hbFBEic/wCmEN/UqqLpgD4rkl33xoDFDd4lkquzQM=");

    assertThatCode(properties::valueCheck).doesNotThrowAnyException();
  }
}
