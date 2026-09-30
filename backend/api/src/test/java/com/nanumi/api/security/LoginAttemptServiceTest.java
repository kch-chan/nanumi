package com.nanumi.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nanumi.api.exception.CustomException;
import com.nanumi.api.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 시계를 직접 쥐고 시간을 흘려보냄. Thread.sleep 을 쓰지 않아 테스트가 빠르고 흔들리지 않음
@DisplayName("로그인 시도 제한")
class LoginAttemptServiceTest {

  private static final String EMAIL = "nanumi@example.com";
  private static final String IP = "127.0.0.1";

  private MovableClock clock;
  private LoginAttemptService service;

  @BeforeEach
  void setUp() {
    clock = new MovableClock(Instant.parse("2026-01-01T00:00:00Z"));
    service = new LoginAttemptService(clock);
  }

  private void fail(int times) {
    for (int i = 0; i < times; i++) {
      service.recordFailure(EMAIL, IP);
    }
  }

  @Test
  @DisplayName("처음에는 막히지 않음")
  void 처음은_통과() {
    assertThat(service.isBlocked(EMAIL, IP)).isFalse();
    assertThatCode(() -> service.checkBlocked(EMAIL, IP)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("이메일로 5번 틀리면 막힘")
  void 이메일_5회() {
    fail(4);
    assertThat(service.isBlocked(EMAIL, IP)).isFalse();

    fail(1);
    assertThat(service.isBlocked(EMAIL, IP)).isTrue();
  }

  @Test
  @DisplayName("막히면 429 를 던짐")
  void 막히면_예외() {
    fail(5);

    assertThatThrownBy(() -> service.checkBlocked(EMAIL, IP))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.LOGIN_ATTEMPT_EXCEEDED);
  }

  // 이메일과 IP 를 한 키로 묶으면 IP 만 바꿔도 카운터가 새로 시작해서 정작 막고 싶던 공격을 못 막음
  @Test
  @DisplayName("IP 를 바꿔도 같은 이메일이면 계속 막힘")
  void IP_를_바꿔도_막힘() {
    fail(5);

    assertThat(service.isBlocked(EMAIL, "10.0.0.9")).isTrue();
  }

  // 아파트는 공유기를 여러 세대가 같이 쓸 수 있어 IP 임계값을 넉넉히 둠
  @Test
  @DisplayName("계정을 바꿔 가며 찔러도 같은 IP 로 20번이면 막힘")
  void IP_20회() {
    for (int i = 0; i < 19; i++) {
      service.recordFailure("user" + i + "@example.com", IP);
    }
    assertThat(service.isBlocked("새계정@example.com", IP)).isFalse();

    service.recordFailure("user19@example.com", IP);
    assertThat(service.isBlocked("새계정@example.com", IP)).isTrue();
  }

  @Test
  @DisplayName("10분이 지나면 잠금이 풀림")
  void 잠금_해제() {
    fail(5);
    assertThat(service.isBlocked(EMAIL, IP)).isTrue();

    clock.plus(Duration.ofMinutes(10).plusSeconds(1));

    assertThat(service.isBlocked(EMAIL, IP)).isFalse();
  }

  @Test
  @DisplayName("30분 동안 아무 일 없으면 실패 기록이 사라짐")
  void 기록_만료() {
    fail(4);

    clock.plus(Duration.ofMinutes(30).plusSeconds(1));

    assertThat(service.getRemainingAttempts(EMAIL, IP)).isEqualTo(5);
  }

  // 비밀번호를 맞혔으면 무차별 대입이 아니므로 그 계정의 실패 기록은 지움
  @Test
  @DisplayName("로그인에 성공하면 그 계정 기록만 지워짐")
  void 성공하면_초기화() {
    fail(4);
    assertThat(service.getRemainingAttempts(EMAIL, IP)).isEqualTo(1);

    service.recordSuccess(EMAIL, IP);

    assertThat(service.getRemainingAttempts(EMAIL, IP)).isEqualTo(5);
  }

  // IP 쪽까지 지우면 계정 하나만 제대로 맞히고 나머지를 계속 찔러 볼 수 있게 됨
  @Test
  @DisplayName("성공해도 IP 기록은 남음")
  void 성공해도_IP_는_유지() {
    for (int i = 0; i < 20; i++) {
      service.recordFailure("user" + i + "@example.com", IP);
    }

    service.recordSuccess("user0@example.com", IP);

    assertThat(service.isBlocked("또다른@example.com", IP)).isTrue();
  }

  // abc@a.com 과 ABC@A.COM 이 따로 세어지면 대소문자만 바꿔 가며 잠금을 피할 수 있음
  @Test
  @DisplayName("이메일 대소문자는 같은 것으로 셈")
  void 대소문자_무시() {
    for (int i = 0; i < 5; i++) {
      service.recordFailure("NANUMI@Example.com", IP);
    }

    assertThat(service.isBlocked("nanumi@example.com", "10.0.0.9")).isTrue();
  }

  @Test
  @DisplayName("남은 횟수는 막히면 0 임")
  void 남은_횟수() {
    assertThat(service.getRemainingAttempts(EMAIL, IP)).isEqualTo(5);

    fail(3);
    assertThat(service.getRemainingAttempts(EMAIL, IP)).isEqualTo(2);

    fail(2);
    assertThat(service.getRemainingAttempts(EMAIL, IP)).isZero();
  }

  @Test
  @DisplayName("이메일이 null 이어도 터지지 않음")
  void null_키() {
    assertThatCode(
            () -> {
              service.recordFailure(null, null);
              service.isBlocked(null, null);
              service.recordSuccess(null, null);
            })
        .doesNotThrowAnyException();
  }

  // 테스트에서 시간을 앞으로 밀 수 있는 시계
  private static final class MovableClock extends Clock {

    private Instant now;

    private MovableClock(Instant start) {
      this.now = start;
    }

    private void plus(Duration duration) {
      this.now = this.now.plus(duration);
    }

    @Override
    public ZoneOffset getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
