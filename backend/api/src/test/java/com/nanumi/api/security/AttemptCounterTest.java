package com.nanumi.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 로그인 잠금(LoginAttemptService)과 가입 제한(SignupAttemptService)이 함께 쓰는 계수기임.
// 두 서비스의 테스트는 "5회면 막힌다" 같은 정책을 보고, 여기서는 계수기 자체의 규칙을 봄
//
// 패키지 전용(package-private) 클래스라 같은 패키지에 두어야 테스트할 수 있음
@DisplayName("시도 계수기")
class AttemptCounterTest {

  private static final Instant NOW = Instant.parse("2026-10-06T00:00:00Z");

  private static final Duration BLOCK = Duration.ofMinutes(10);
  private static final Duration TTL = Duration.ofMinutes(30);

  private AttemptCounter counter(int maxAttempts, int maxEntries) {
    return new AttemptCounter(maxAttempts, BLOCK, TTL, maxEntries);
  }

  @Test
  @DisplayName("기록이 없으면 막히지 않음")
  void 처음은_통과() {
    AttemptCounter counter = counter(3, 100);

    assertThat(counter.isBlocked("a", NOW)).isFalse();
    assertThat(counter.remaining("a", NOW)).isEqualTo(3);
  }

  @Test
  @DisplayName("임계값 직전까지는 통과함")
  void 임계값_직전() {
    AttemptCounter counter = counter(3, 100);

    counter.recordFailure("a", NOW);
    counter.recordFailure("a", NOW);

    assertThat(counter.isBlocked("a", NOW)).isFalse();
    assertThat(counter.remaining("a", NOW)).isEqualTo(1);
  }

  @Test
  @DisplayName("임계값에 닿으면 막힘")
  void 임계값_도달() {
    AttemptCounter counter = counter(3, 100);

    for (int i = 0; i < 3; i++) {
      counter.recordFailure("a", NOW);
    }

    assertThat(counter.isBlocked("a", NOW)).isTrue();
    assertThat(counter.remaining("a", NOW)).isZero();
  }

  @Test
  @DisplayName("차단 시간이 지나면 풀림")
  void 차단_해제() {
    AttemptCounter counter = counter(3, 100);
    for (int i = 0; i < 3; i++) {
      counter.recordFailure("a", NOW);
    }

    assertThat(counter.isBlocked("a", NOW.plus(BLOCK).minusSeconds(1))).isTrue();
    assertThat(counter.isBlocked("a", NOW.plus(BLOCK))).isFalse();
  }

  // 풀린 뒤에는 처음부터 다시 세야 함
  // 남은 횟수가 0 인 채로 남아 있으면 한 번만 더 틀려도 바로 막히게 됨
  @Test
  @DisplayName("차단이 풀리면 횟수도 초기화됨")
  void 해제_뒤_초기화() {
    AttemptCounter counter = counter(3, 100);
    for (int i = 0; i < 3; i++) {
      counter.recordFailure("a", NOW);
    }

    Instant after = NOW.plus(BLOCK);
    counter.isBlocked("a", after);

    assertThat(counter.remaining("a", after)).isEqualTo(3);
  }

  // 실패한 채로 한참 지난 기록은 버림
  // 어제 두 번 틀린 것이 오늘까지 따라오면, 오타 한 번에 막히는 사람이 생김
  @Test
  @DisplayName("오래 방치된 기록은 처음부터 다시 셈")
  void 기록_만료() {
    AttemptCounter counter = counter(3, 100);
    counter.recordFailure("a", NOW);
    counter.recordFailure("a", NOW);

    Instant later = NOW.plus(TTL);

    assertThat(counter.remaining("a", later)).isEqualTo(3);
    counter.recordFailure("a", later);
    assertThat(counter.remaining("a", later)).isEqualTo(2);
  }

  @Test
  @DisplayName("키를 지우면 기록이 사라짐")
  void 기록_삭제() {
    AttemptCounter counter = counter(3, 100);
    for (int i = 0; i < 3; i++) {
      counter.recordFailure("a", NOW);
    }

    counter.clear("a");

    assertThat(counter.isBlocked("a", NOW)).isFalse();
    assertThat(counter.remaining("a", NOW)).isEqualTo(3);
  }

  @Test
  @DisplayName("키마다 따로 셈")
  void 키별_독립() {
    AttemptCounter counter = counter(3, 100);
    for (int i = 0; i < 3; i++) {
      counter.recordFailure("a", NOW);
    }

    assertThat(counter.isBlocked("a", NOW)).isTrue();
    assertThat(counter.isBlocked("b", NOW)).isFalse();
  }

  // 상한이 없으면 키를 계속 바꿔 가며 요청하는 것만으로 메모리를 고갈시킬 수 있음
  // 접근 순서 지도라서 상한을 넘으면 가장 오래 안 쓴 항목이 먼저 밀려남
  @Test
  @DisplayName("보관 상한을 넘으면 오래된 기록부터 밀려남")
  void 보관_상한() {
    AttemptCounter counter = counter(1, 2);

    counter.recordFailure("first", NOW);
    counter.recordFailure("second", NOW);
    counter.recordFailure("third", NOW);

    assertThat(counter.isBlocked("third", NOW)).isTrue();
    assertThat(counter.isBlocked("second", NOW)).isTrue();
    assertThat(counter.isBlocked("first", NOW)).isFalse();
  }
}
