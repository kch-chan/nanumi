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
  private static final int MAX_ATTEMPTS = 5;

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
    fail(MAX_ATTEMPTS - 1);

    assertThat(loginAttemptService.isBlocked(EMAIL, IP)).isFalse();
  }

  @Test
  @DisplayName("다섯 번 실패하면 막힘")
  void 다섯번_실패하면_막힘() {
    fail(MAX_ATTEMPTS);

    assertThat(loginAttemptService.isBlocked(EMAIL, IP)).isTrue();
  }

  @Test
  @DisplayName("막힌 상태에서 확인하면 시도 초과 오류가 남")
  void 막히면_예외를_던짐() {
    fail(MAX_ATTEMPTS);

    assertThatThrownBy(() -> loginAttemptService.checkBlocked(EMAIL, IP))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_ATTEMPT_EXCEEDED);
  }

  @Test
  @DisplayName("막히지 않았으면 확인해도 아무 일이 없음")
  void 안_막혔으면_통과함() {
    fail(MAX_ATTEMPTS - 1);

    assertThatCode(() -> loginAttemptService.checkBlocked(EMAIL, IP)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("10분이 지나면 다시 열림")
  void 차단_시간이_지나면_풀림() {
    fail(MAX_ATTEMPTS);
    clock.advance(Duration.ofMinutes(10));

    assertThat(loginAttemptService.isBlocked(EMAIL, IP)).isFalse();
  }

  @Test
  @DisplayName("10분이 되기 전에는 계속 막혀 있음")
  void 차단_시간_전에는_막혀_있음() {
    fail(MAX_ATTEMPTS);
    clock.advance(Duration.ofMinutes(9));

    assertThat(loginAttemptService.isBlocked(EMAIL, IP)).isTrue();
  }

  @Test
  @DisplayName("차단이 풀리면 시도 횟수도 처음부터 다시 셈")
  void 차단이_풀리면_횟수도_초기화됨() {
    fail(MAX_ATTEMPTS);
    clock.advance(Duration.ofMinutes(10));
    loginAttemptService.isBlocked(EMAIL, IP);

    fail(MAX_ATTEMPTS - 1);

    assertThat(loginAttemptService.isBlocked(EMAIL, IP)).isFalse();
  }

  @Test
  @DisplayName("로그인에 성공하면 그동안의 실패는 없던 일이 됨")
  void 성공하면_기록이_지워짐() {
    fail(MAX_ATTEMPTS - 1);
    loginAttemptService.recordSuccess(EMAIL, IP);
    fail(MAX_ATTEMPTS - 1);

    assertThat(loginAttemptService.isBlocked(EMAIL, IP)).isFalse();
  }

  @Test
  @DisplayName("이메일이 다르면 따로 셈")
  void 이메일이_다르면_따로_셈() {
    fail(MAX_ATTEMPTS);

    assertThat(loginAttemptService.isBlocked("other@example.com", IP)).isFalse();
  }

  @Test
  @DisplayName("같은 이메일이라도 IP 가 다르면 따로 셈")
  void IP가_다르면_따로_셈() {
    fail(MAX_ATTEMPTS);

    assertThat(loginAttemptService.isBlocked(EMAIL, "10.0.0.1")).isFalse();
  }

  @Test
  @DisplayName("이메일 대소문자만 바꿔서는 차단을 피할 수 없음")
  void 이메일_대소문자는_같게_셈() {
    fail(MAX_ATTEMPTS);

    assertThat(loginAttemptService.isBlocked("NANUMI@EXAMPLE.COM", IP)).isTrue();
  }

  @Test
  @DisplayName("실패한 채로 30분이 지나면 처음부터 다시 셈")
  void 오래된_실패는_초기화됨() {
    fail(MAX_ATTEMPTS - 1);
    clock.advance(Duration.ofMinutes(30));
    fail(1);

    assertThat(loginAttemptService.isBlocked(EMAIL, IP)).isFalse();
  }

  @Test
  @DisplayName("남은 시도 횟수를 알려 줌")
  void 남은_시도_횟수를_알려_줌() {
    assertThat(loginAttemptService.getRemainingAttempts(EMAIL, IP)).isEqualTo(MAX_ATTEMPTS);

    fail(2);

    assertThat(loginAttemptService.getRemainingAttempts(EMAIL, IP)).isEqualTo(MAX_ATTEMPTS - 2);
  }

  @Test
  @DisplayName("막힌 뒤에는 남은 시도 횟수가 0 임")
  void 막히면_남은_시도가_없음() {
    fail(MAX_ATTEMPTS);

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
