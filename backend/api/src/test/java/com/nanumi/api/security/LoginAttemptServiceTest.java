package com.nanumi.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nanumi.api.exception.CustomException;
import com.nanumi.api.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("로그인 시도 차단")
class LoginAttemptServiceTest {

  private static final String EMAIL = "nanumi@example.com";
  private static final String IP = "127.0.0.1";
  private static final int EMAIL_MAX_ATTEMPTS = 5;
  private static final int IP_MAX_ATTEMPTS = 20;

  private MutableClock clock;
  private LoginAttemptService loginAttemptService;

  @BeforeEach
  void setUp() {
    clock = new MutableClock(Instant.parse("2026-09-01T00:00:00Z"));
    loginAttemptService = new LoginAttemptService(clock);
  }

  private void fail(int times) {
    for (int i = 0; i < times; i++) {
      loginAttemptService.recordFailure(EMAIL, IP);
    }
  }

  @Test
  @DisplayName("한 번도 실패하지 않았으면 막히지 않음")
  void 처음에는_막히지_않음() {
    assertThat(loginAttemptService.isBlocked(EMAIL, IP)).isFalse();
  }

  @Test
  @DisplayName("네 번까지는 막히지 않음")
  void 네번_실패해도_막히지_않음() {
    fail(EMAIL_MAX_ATTEMPTS - 1);

    assertThat(loginAttemptService.isBlocked(EMAIL, IP)).isFalse();
  }

  @Test
  @DisplayName("한 계정으로 다섯 번 실패하면 막힘")
  void 다섯번_실패하면_막힘() {
    fail(EMAIL_MAX_ATTEMPTS);

    assertThat(loginAttemptService.isBlocked(EMAIL, IP)).isTrue();
  }

  @Test
  @DisplayName("막힌 상태에서 확인하면 시도 초과 오류가 남")
  void 막히면_예외를_던짐() {
    fail(EMAIL_MAX_ATTEMPTS);

    assertThatThrownBy(() -> loginAttemptService.checkBlocked(EMAIL, IP))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_ATTEMPT_EXCEEDED);
  }

  @Test
  @DisplayName("막히지 않았으면 확인해도 아무 일이 없음")
  void 안_막혔으면_통과함() {
    fail(EMAIL_MAX_ATTEMPTS - 1);

    assertThatCode(() -> loginAttemptService.checkBlocked(EMAIL, IP)).doesNotThrowAnyException();
  }

  // 이게 이 클래스에서 제일 중요한 테스트임
  // 예전에는 이메일과 IP 를 한 키로 묶어서, IP 만 바꾸면 카운터가 새로 시작됐음
  // 정작 막고 싶던 "한 명이 IP 를 바꿔 가며 한 계정을 찔러 보는 공격"이 그대로 통했다는 뜻임
  @Test
  @DisplayName("IP 를 바꿔도 그 계정의 실패 횟수는 그대로 남음")
  void IP를_바꿔도_계정_카운터는_유지됨() {
    fail(EMAIL_MAX_ATTEMPTS);

    assertThat(loginAttemptService.isBlocked(EMAIL, "10.0.0.1")).isTrue();
    assertThat(loginAttemptService.isBlocked(EMAIL, "203.0.113.9")).isTrue();
  }

  @Test
  @DisplayName("한 IP 에서 계정을 바꿔 가며 시도하면 IP 쪽에서 막힘")
  void 계정을_바꿔가며_시도하면_IP로_막힘() {
    for (int i = 0; i < IP_MAX_ATTEMPTS; i++) {
      loginAttemptService.recordFailure("user" + i + "@example.com", IP);
    }

    // 계정별로는 한 번씩만 틀렸는데도 같은 회선에서 온 시도라 막힘
    assertThat(loginAttemptService.isBlocked("user0@example.com", IP)).isTrue();
    assertThat(loginAttemptService.isBlocked("아직안쓴@example.com", IP)).isTrue();
  }

  @Test
  @DisplayName("IP 임계값은 계정보다 넉넉해서 공유기를 같이 쓰는 이웃이 바로 막히지 않음")
  void IP_임계값이_더_넉넉함() {
    for (int i = 0; i < EMAIL_MAX_ATTEMPTS + 1; i++) {
      loginAttemptService.recordFailure("user" + i + "@example.com", IP);
    }

    assertThat(loginAttemptService.isBlocked("이웃@example.com", IP)).isFalse();
  }

  @Test
  @DisplayName("10분이 지나면 다시 열림")
  void 차단_시간이_지나면_풀림() {
    fail(EMAIL_MAX_ATTEMPTS);
    clock.advance(Duration.ofMinutes(10));

    assertThat(loginAttemptService.isBlocked(EMAIL, IP)).isFalse();
  }

  @Test
  @DisplayName("10분이 되기 전에는 계속 막혀 있음")
  void 차단_시간_전에는_막혀_있음() {
    fail(EMAIL_MAX_ATTEMPTS);
    clock.advance(Duration.ofMinutes(9));

    assertThat(loginAttemptService.isBlocked(EMAIL, IP)).isTrue();
  }

  @Test
  @DisplayName("차단이 풀리면 시도 횟수도 처음부터 다시 셈")
  void 차단이_풀리면_횟수도_초기화됨() {
    fail(EMAIL_MAX_ATTEMPTS);
    clock.advance(Duration.ofMinutes(10));
    loginAttemptService.isBlocked(EMAIL, IP);

    fail(EMAIL_MAX_ATTEMPTS - 1);

    assertThat(loginAttemptService.isBlocked(EMAIL, IP)).isFalse();
  }

  @Test
  @DisplayName("로그인에 성공하면 그 계정의 실패 기록은 없던 일이 됨")
  void 성공하면_계정_기록이_지워짐() {
    fail(EMAIL_MAX_ATTEMPTS - 1);
    loginAttemptService.recordSuccess(EMAIL, IP);
    fail(EMAIL_MAX_ATTEMPTS - 1);

    assertThat(loginAttemptService.isBlocked(EMAIL, IP)).isFalse();
  }

  @Test
  @DisplayName("다른 계정의 실패는 내 계정 카운터에 쌓이지 않음")
  void 이메일이_다르면_따로_셈() {
    fail(EMAIL_MAX_ATTEMPTS);

    assertThat(loginAttemptService.isBlocked("other@example.com", "10.0.0.1")).isFalse();
  }

  @Test
  @DisplayName("이메일 대소문자만 바꿔서는 차단을 피할 수 없음")
  void 이메일_대소문자는_같게_셈() {
    fail(EMAIL_MAX_ATTEMPTS);

    assertThat(loginAttemptService.isBlocked("NANUMI@EXAMPLE.COM", IP)).isTrue();
  }

  @Test
  @DisplayName("실패한 채로 30분이 지나면 처음부터 다시 셈")
  void 오래된_실패는_초기화됨() {
    fail(EMAIL_MAX_ATTEMPTS - 1);
    clock.advance(Duration.ofMinutes(30));
    fail(1);

    assertThat(loginAttemptService.isBlocked(EMAIL, IP)).isFalse();
  }

  @Test
  @DisplayName("남은 시도 횟수를 알려 줌")
  void 남은_시도_횟수를_알려_줌() {
    assertThat(loginAttemptService.getRemainingAttempts(EMAIL, IP)).isEqualTo(EMAIL_MAX_ATTEMPTS);

    fail(2);

    assertThat(loginAttemptService.getRemainingAttempts(EMAIL, IP))
        .isEqualTo(EMAIL_MAX_ATTEMPTS - 2);
  }

  @Test
  @DisplayName("막힌 뒤에는 남은 시도 횟수가 0 임")
  void 막히면_남은_시도가_없음() {
    fail(EMAIL_MAX_ATTEMPTS);

    assertThat(loginAttemptService.getRemainingAttempts(EMAIL, IP)).isZero();
  }

  // 차단이 풀리는지 보려면 시간을 앞으로 밀 수 있어야 해서 직접 만듦
  private static final class MutableClock extends Clock {

    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    private void advance(Duration duration) {
      this.instant = this.instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}
