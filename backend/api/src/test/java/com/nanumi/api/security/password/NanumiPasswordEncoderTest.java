package com.nanumi.api.security.password;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@DisplayName("나누미 비밀번호 인코더")
class NanumiPasswordEncoderTest {

  // 테스트가 오래 걸리지 않도록 반복 횟수를 낮춰서 씀
  private static final int TEST_ITERATIONS = 1_000;
  private static final String TEST_PEPPER = "test-pepper";
  private static final String RAW_PASSWORD = "nanumi1234!";

  private final NanumiPasswordEncoder encoder = encoderWith(TEST_ITERATIONS, TEST_PEPPER);

  private static NanumiPasswordEncoder encoderWith(int iterations, String pepper) {
    NanumiPasswordProperties properties = new NanumiPasswordProperties();
    properties.setIterations(iterations);
    properties.setPepper(pepper);
    return new NanumiPasswordEncoder(properties);
  }

  @Test
  @DisplayName("해시는 접두사·버전·반복 횟수·salt·hash 다섯 조각으로 이루어짐")
  void 해시_형식이_규칙대로임() {
    String encoded = encoder.encode(RAW_PASSWORD);

    assertThat(encoded).startsWith("$nanumi$1$" + TEST_ITERATIONS + "$");
    assertThat(encoded.split("\\$")).hasSize(6);
  }

  @Test
  @DisplayName("기본 반복 횟수로 해싱하면 accounts.password 컬럼 길이인 83자가 나옴")
  void 기본_설정이면_83자임() {
    NanumiPasswordProperties properties = new NanumiPasswordProperties();
    NanumiPasswordEncoder defaultEncoder = new NanumiPasswordEncoder(properties);

    assertThat(defaultEncoder.encode(RAW_PASSWORD)).hasSize(83);
  }

  @Test
  @DisplayName("같은 비밀번호라도 salt 가 달라서 해시가 매번 다름")
  void 같은_비밀번호도_해시가_매번_다름() {
    assertThat(encoder.encode(RAW_PASSWORD)).isNotEqualTo(encoder.encode(RAW_PASSWORD));
  }

  @Test
  @DisplayName("해시가 달라도 원래 비밀번호로는 둘 다 열림")
  void 해시가_달라도_같은_비밀번호로_열림() {
    assertThat(encoder.matches(RAW_PASSWORD, encoder.encode(RAW_PASSWORD))).isTrue();
    assertThat(encoder.matches(RAW_PASSWORD, encoder.encode(RAW_PASSWORD))).isTrue();
  }

  @Test
  @DisplayName("맞는 비밀번호면 통과함")
  void 맞는_비밀번호면_참임() {
    assertThat(encoder.matches(RAW_PASSWORD, encoder.encode(RAW_PASSWORD))).isTrue();
  }

  @Test
  @DisplayName("틀린 비밀번호면 막힘")
  void 틀린_비밀번호면_거짓임() {
    assertThat(encoder.matches("nanumi1234?", encoder.encode(RAW_PASSWORD))).isFalse();
  }

  @Test
  @DisplayName("대소문자가 다르면 다른 비밀번호로 봄")
  void 대소문자를_구분함() {
    assertThat(encoder.matches("Nanumi1234!", encoder.encode(RAW_PASSWORD))).isFalse();
  }

  @Test
  @DisplayName("비밀번호가 null 이면 맞춰 볼 것도 없이 막힘")
  void 입력이_null_이면_거짓임() {
    assertThat(encoder.matches(null, encoder.encode(RAW_PASSWORD))).isFalse();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("저장된 해시가 비어 있으면 막힘")
  void 저장된_해시가_비어_있으면_거짓임(String stored) {
    assertThat(encoder.matches(RAW_PASSWORD, stored)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "그냥평문",
        "$nanumi$1$1000$salt",
        "$nanumi$1$1000$salt$hash$extra",
        "$other$1$1000$c2FsdA$aGFzaA",
        "$nanumi$1$영$c2FsdA$aGFzaA",
        "$nanumi$1$0$c2FsdA$aGFzaA",
        "$nanumi$1$1000$!!!$aGFzaA"
      })
  @DisplayName("형식이 깨진 해시는 예외 없이 그냥 막힘")
  void 형식이_깨진_해시는_거짓임(String stored) {
    assertThat(encoder.matches(RAW_PASSWORD, stored)).isFalse();
  }

  @Test
  @DisplayName("pepper 를 모르면 같은 비밀번호라도 열지 못함")
  void pepper가_다르면_열리지_않음() {
    String encoded = encoder.encode(RAW_PASSWORD);
    NanumiPasswordEncoder otherEncoder = encoderWith(TEST_ITERATIONS, "다른-pepper");

    assertThat(otherEncoder.matches(RAW_PASSWORD, encoded)).isFalse();
  }

  @Test
  @DisplayName("pepper 가 같으면 다른 인스턴스에서도 열림")
  void pepper가_같으면_열림() {
    String encoded = encoder.encode(RAW_PASSWORD);
    NanumiPasswordEncoder sameEncoder = encoderWith(TEST_ITERATIONS, TEST_PEPPER);

    assertThat(sameEncoder.matches(RAW_PASSWORD, encoded)).isTrue();
  }

  @Test
  @DisplayName("반복 횟수를 올려도 예전 해시로 로그인됨")
  void 반복횟수를_올려도_기존_해시가_열림() {
    String encoded = encoder.encode(RAW_PASSWORD);
    NanumiPasswordEncoder strongerEncoder = encoderWith(TEST_ITERATIONS * 2, TEST_PEPPER);

    assertThat(strongerEncoder.matches(RAW_PASSWORD, encoded)).isTrue();
  }

  @Test
  @DisplayName("반복 횟수가 설정보다 낮으면 다시 해싱해야 함")
  void 반복횟수가_낮으면_재해시_대상임() {
    String encoded = encoder.encode(RAW_PASSWORD);
    NanumiPasswordEncoder strongerEncoder = encoderWith(TEST_ITERATIONS * 2, TEST_PEPPER);

    assertThat(strongerEncoder.upgradeEncoding(encoded)).isTrue();
  }

  @Test
  @DisplayName("반복 횟수가 설정과 같으면 다시 해싱하지 않음")
  void 반복횟수가_같으면_재해시_대상이_아님() {
    assertThat(encoder.upgradeEncoding(encoder.encode(RAW_PASSWORD))).isFalse();
  }

  @Test
  @DisplayName("읽지 못하는 해시는 다시 해싱해도 소용없으므로 대상이 아님")
  void 깨진_해시는_재해시_대상이_아님() {
    assertThat(encoder.upgradeEncoding("깨진해시")).isFalse();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("빈 해시는 재해시 대상이 아님")
  void 빈_해시는_재해시_대상이_아님(String stored) {
    assertThat(encoder.upgradeEncoding(stored)).isFalse();
  }

  @Test
  @DisplayName("예전 BCrypt 해시도 로그인은 그대로 됨")
  void 기존_BCrypt_해시로도_열림() {
    String bcryptHash = new BCryptPasswordEncoder().encode(RAW_PASSWORD);

    assertThat(encoder.matches(RAW_PASSWORD, bcryptHash)).isTrue();
    assertThat(encoder.matches("틀린비밀번호1!", bcryptHash)).isFalse();
  }

  @Test
  @DisplayName("BCrypt 해시는 무조건 새 형식으로 갈아탈 대상임")
  void 기존_BCrypt_해시는_재해시_대상임() {
    String bcryptHash = new BCryptPasswordEncoder().encode(RAW_PASSWORD);

    assertThat(encoder.upgradeEncoding(bcryptHash)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"$2a$10$abcdefghijklmnopqrstuv", "$2b$10$x", "$2y$10$x"})
  @DisplayName("BCrypt 접두사를 알아봄")
  void BCrypt_해시를_알아봄(String stored) {
    assertThat(encoder.isLegacyHash(stored)).isTrue();
  }

  @Test
  @DisplayName("새 형식 해시는 예전 해시가 아님")
  void 새_형식은_BCrypt가_아님() {
    assertThat(encoder.isLegacyHash(encoder.encode(RAW_PASSWORD))).isFalse();
    assertThat(encoder.isLegacyHash(null)).isFalse();
  }

  @Test
  @DisplayName("null 을 해싱하려 하면 예외가 남")
  void null_은_해싱하지_못함() {
    assertThatThrownBy(() -> encoder.encode(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("반복 횟수를 0 이하로 두면 해싱할 때 바로 막힘")
  void 반복횟수가_0이면_예외임() {
    NanumiPasswordEncoder brokenEncoder = encoderWith(0, TEST_PEPPER);

    assertThatThrownBy(() -> brokenEncoder.encode(RAW_PASSWORD))
        .isInstanceOf(IllegalStateException.class);
  }
}
