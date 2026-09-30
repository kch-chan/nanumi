package com.nanumi.api.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// 키(이메일·IP)별로 실패 횟수를 세고, 임계값을 넘으면 일정 시간 막는 계수기임
//
// 로그인 잠금과 가입 제한이 같은 방식이라 한 곳에 두고 양쪽에서 씀
//
// 메모리에만 들고 있어서 프로세스가 다시 뜨면 기록이 사라짐.
// Render 무료 등급은 유휴 시 컨테이너를 내리고 배포마다 새로 뜨므로 그때마다 초기화됨.
// 즉 "확실히 막힌다"고 신뢰할 수 있는 장치는 아니고, 공용 저장소(Redis 등)가 있어야 제대로 됨
final class AttemptCounter {

  private final int maxAttempts;
  private final Duration blockDuration;

  // 실패한 채로 한참 지난 기록은 처음부터 다시 셈
  private final Duration attemptTtl;

  private final Map<String, Attempt> attempts;

  AttemptCounter(int maxAttempts, Duration blockDuration, Duration attemptTtl, int maxEntries) {
    this.maxAttempts = maxAttempts;
    this.blockDuration = blockDuration;
    this.attemptTtl = attemptTtl;
    this.attempts = createBoundedMap(maxEntries);
  }

  boolean isBlocked(String key, Instant now) {
    Attempt attempt = attempts.get(key);
    if (attempt == null) {
      return false;
    }

    if (attempt.blockedUntil != null && now.isBefore(attempt.blockedUntil)) {
      return true;
    }

    // 차단이 풀렸거나 오래 방치된 기록이면 지워서 처음부터 다시 세도록 함
    if (isStale(attempt, now)) {
      attempts.remove(key, attempt);
    }
    return false;
  }

  void recordFailure(String key, Instant now) {
    attempts.compute(
        key,
        (ignored, current) -> {
          Attempt attempt = (current == null || isStale(current, now)) ? new Attempt() : current;
          attempt.failures++;
          attempt.lastFailureAt = now;
          if (attempt.failures >= maxAttempts) {
            attempt.blockedUntil = now.plus(blockDuration);
          }
          return attempt;
        });
  }

  void clear(String key) {
    attempts.remove(key);
  }

  int remaining(String key, Instant now) {
    Attempt attempt = attempts.get(key);
    if (attempt == null || isStale(attempt, now)) {
      return maxAttempts;
    }
    return Math.max(0, maxAttempts - attempt.failures);
  }

  private boolean isStale(Attempt attempt, Instant now) {
    if (attempt.blockedUntil != null) {
      return !now.isBefore(attempt.blockedUntil);
    }
    return attempt.lastFailureAt == null || !now.isBefore(attempt.lastFailureAt.plus(attemptTtl));
  }

  // 접근 순서로 정렬되는 지도라서, 상한을 넘으면 가장 오래 안 쓴 항목이 먼저 밀려남
  // 상한이 없으면 키를 계속 바꿔 가며 요청하는 것만으로 메모리를 고갈시킬 수 있음
  // 밀려난 자리에 차단 기록이 있었다면 그 키는 다시 처음부터 세게 됨.
  // 메모리가 무한정 늘어나는 것보다는 낫다고 보고 감수함
  private static Map<String, Attempt> createBoundedMap(int maxEntries) {
    return Collections.synchronizedMap(
        new LinkedHashMap<String, Attempt>(64, 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<String, Attempt> eldest) {
            return size() > maxEntries;
          }
        });
  }

  private static final class Attempt {
    private int failures;
    private Instant lastFailureAt;
    private Instant blockedUntil;
  }
}
