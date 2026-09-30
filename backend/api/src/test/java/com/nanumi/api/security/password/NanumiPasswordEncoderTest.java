package com.nanumi.api.security.password;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 스프링을 띄우지 않고 객체만 만들어서 확인함. DB 도 필요 없음
@DisplayName("비밀번호 인코더")
class NanumiPasswordEncoderTest {

  private NanumiPasswordEncoder encoder;

  @BeforeEach
  void setUp() {
    NanumiPasswordProperties properties = new NanumiPasswordProperties();
    // 테스트는 빨라야 하므로 반복 횟수를 낮춤. 형식과 동작은 그대로임
    properties.setIterations(1000);
    properties.setPepper("test-pepper");
    encoder = new NanumiPasswordEncoder(properties);
  }

  @Test
  @DisplayName("해시한 비밀번호는 원문과 다르고, 다시 검증하면 통과함")
  void 해시하고_검증함() {
    String encoded = encoder.encode("Ab3!efgh");

    assertThat(encoded).isNotEqualTo("Ab3!efgh");
    assertThat(encoder.matches("Ab3!efgh", encoded)).isTrue();
  }

  @Test
  @DisplayName("비밀번호가 다르면 검증에 실패함")
  void 다른_비밀번호는_실패함() {
    String encoded = encoder.encode("Ab3!efgh");

    assertThat(encoder.matches("Ab3!efgi", encoded)).isFalse();
  }

  // salt 가 매번 새로 뽑히므로 같은 비밀번호라도 해시가 달라져야 함
  // 같다면 DB 만 보고 같은 비밀번호를 쓰는 계정을 골라낼 수 있게 됨
  @Test
  @DisplayName("같은 비밀번호라도 해시는 매번 다름")
  void 같은_비밀번호도_해시는_다름() {
    assertThat(encoder.encode("Ab3!efgh")).isNotEqualTo(encoder.encode("Ab3!efgh"));
  }

  @Test
  @DisplayName("해시는 $nanumi$버전$반복횟수$salt$hash 형식임")
  void 정해진_형식으로_만들어짐() {
    String encoded = encoder.encode("Ab3!efgh");

    assertThat(encoded).startsWith("$nanumi$1$1000$");
    assertThat(encoded.chars().filter(c -> c == (int) 0x24).count()).isEqualTo(5);
  }

  // pepper 는 DB 에 저장되지 않고 서버만 가지고 있음
  // 그래서 DB 가 통째로 유출돼도 pepper 를 모르면 대입 공격이 통하지 않아야 함
  @Test
  @DisplayName("pepper 가 다르면 같은 비밀번호라도 검증에 실패함")
  void pepper_가_다르면_실패함() {
    String encoded = encoder.encode("Ab3!efgh");

    NanumiPasswordProperties other = new NanumiPasswordProperties();
    other.setIterations(1000);
    other.setPepper("다른-pepper");

    assertThat(new NanumiPasswordEncoder(other).matches("Ab3!efgh", encoded)).isFalse();
  }
}
