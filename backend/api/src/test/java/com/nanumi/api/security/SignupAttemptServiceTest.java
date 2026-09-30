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

@DisplayName("가입 시도 제한")
class SignupAttemptServiceTest {

  private static final String IP = "10.0.0.1";

  // 시간을 직접 흘려보내려고 고정 시계를 씀. 테스트가 실제로 기다리지 않게 하기 위함임
  private Instant now;
  private SignupAttemptService service;

  @BeforeEach
  void setUp() {
    now = Instant.parse("2026-01-01T00:00:00Z");
    service = new SignupAttemptService(movableClock());
  }

  private Clock movableClock() {
    return new Clock() {
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
    };
  }

  @Test
  @DisplayName("처음에는 다섯 번 남아 있음")
  void 기본_잔여_횟수() {
    assertThat(service.getRemainingAttempts(IP)).isEqualTo(SignupAttemptService.IP_MAX_ATTEMPTS);
    assertThatCode(() -> service.checkBlocked(IP)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("네 번까지는 통과함")
  void 네_번은_통과() {
    for (int i = 0; i < 4; i++) {
      service.recordAttempt(IP);
    }

    assertThat(service.getRemainingAttempts(IP)).isEqualTo(1);
    assertThatCode(() -> service.checkBlocked(IP)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("다섯 번을 넘기면 막힘")
  void 다섯_번_넘으면_막힘() {
    for (int i = 0; i < 5; i++) {
      service.recordAttempt(IP);
    }

    assertThat(service.getRemainingAttempts(IP)).isZero();
    assertThatThrownBy(() -> service.checkBlocked(IP))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.SIGNUP_ATTEMPT_EXCEEDED);
  }

  // 한 시간 지나면 다시 가입할 수 있어야 함. 영구 차단이 아님
  @Test
  @DisplayName("한 시간 지나면 다시 됨")
  void 한_시간_뒤_해제() {
    for (int i = 0; i < 5; i++) {
      service.recordAttempt(IP);
    }

    now = now.plus(Duration.ofHours(1)).plusSeconds(1);

    assertThatCode(() -> service.checkBlocked(IP)).doesNotThrowAnyException();
    assertThat(service.getRemainingAttempts(IP)).isEqualTo(SignupAttemptService.IP_MAX_ATTEMPTS);
  }

  // 한 IP 가 막혀도 다른 집(IP)은 가입할 수 있어야 함
  @Test
  @DisplayName("다른 IP 는 영향을 받지 않음")
  void 다른_IP_는_따로_셈() {
    for (int i = 0; i < 5; i++) {
      service.recordAttempt(IP);
    }

    assertThatCode(() -> service.checkBlocked("10.0.0.2")).doesNotThrowAnyException();
  }
}
