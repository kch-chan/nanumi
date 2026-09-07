package com.nanumi.api.security;

import com.nanumi.api.exception.CustomException;
import com.nanumi.api.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

// 로그인 무차별 대입을 막는 카운터임
//
// 이메일 하나만 세면 한 명이 IP 를 바꿔 가며 계속 시도할 수 있고,
// IP 하나만 세면 같은 공유기를 쓰는 다른 입주민까지 막히므로 이메일 + IP 를 묶어서 셈
// 5회 연속 실패하면 10분 동안 막고, 성공하면 기록을 지움
//
// 메모리에만 들고 있어서 서버를 여러 대로 늘리면 인스턴스별로 따로 셈.
// 그때는 Redis 같은 공용 저장소로 옮겨야 함
@Component
public class LoginAttemptService {

  private static final int MAX_ATTEMPTS = 5;
  private static final Duration BLOCK_DURATION = Duration.ofMinutes(10);

  // 실패한 채로 한참 지난 기록은 처음부터 다시 셈
  private static final Duration ATTEMPT_TTL = Duration.ofMinutes(30);

  // 기록이 이만큼 쌓이면 지나간 것들을 한 번 훑어서 지움
  private static final int CLEANUP_THRESHOLD = 1_000;

  private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
  private final Clock clock;

  public LoginAttemptService() {
    this(Clock.systemDefaultZone());
  }

  // 테스트에서 시간을 직접 흘려보내려고 열어 둔 생성자임
  LoginAttemptService(Clock clock) {
    this.clock = clock;
  }

  // 막혀 있으면 예외를 던짐. 로그인 처리 맨 앞에서 부름
  public void checkBlocked(String email, String clientIp) {
    if (isBlocked(email, clientIp)) {
      throw new CustomException(ErrorCode.LOGIN_ATTEMPT_EXCEEDED);
    }
  }

  public boolean isBlocked(String email, String clientIp) {
    String key = key(email, clientIp);
    Attempt attempt = attempts.get(key);
    if (attempt == null) {
      return false;
    }

    Instant now = clock.instant();
    if (attempt.blockedUntil != null && now.isBefore(attempt.blockedUntil)) {
      return true;
    }

    // 차단이 풀렸거나 오래 방치된 기록이면 지워서 다시 5회를 주도록 함
    if (isStale(attempt, now)) {
      attempts.remove(key, attempt);
    }
    return false;
  }

  public void recordFailure(String email, String clientIp) {
    Instant now = clock.instant();

    attempts.compute(
        key(email, clientIp),
        (ignored, current) -> {
          Attempt attempt = (current == null || isStale(current, now)) ? new Attempt() : current;
          attempt.failures++;
          attempt.lastFailureAt = now;
          if (attempt.failures >= MAX_ATTEMPTS) {
            attempt.blockedUntil = now.plus(BLOCK_DURATION);
          }
          return attempt;
        });

    cleanUp(now);
  }

  // 로그인에 성공하면 지금까지의 실패는 없던 일로 함
  public void recordSuccess(String email, String clientIp) {
    attempts.remove(key(email, clientIp));
  }

  // 남은 시도 횟수임. 이미 막혀 있으면 0 임
  public int getRemainingAttempts(String email, String clientIp) {
    Attempt attempt = attempts.get(key(email, clientIp));
    if (attempt == null) {
      return MAX_ATTEMPTS;
    }
    if (isStale(attempt, clock.instant())) {
      return MAX_ATTEMPTS;
    }
    return Math.max(0, MAX_ATTEMPTS - attempt.failures);
  }

  // 이메일은 대소문자를 가리지 않으므로 소문자로 맞춰서 셈
  private String key(String email, String clientIp) {
    String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    String normalizedIp = clientIp == null ? "" : clientIp.trim();
    return normalizedEmail + "|" + normalizedIp;
  }

  private boolean isStale(Attempt attempt, Instant now) {
    if (attempt.blockedUntil != null) {
      return !now.isBefore(attempt.blockedUntil);
    }
    return attempt.lastFailureAt == null || !now.isBefore(attempt.lastFailureAt.plus(ATTEMPT_TTL));
  }

  private void cleanUp(Instant now) {
    if (attempts.size() < CLEANUP_THRESHOLD) {
      return;
    }
    attempts.entrySet().removeIf(entry -> isStale(entry.getValue(), now));
  }

  private static final class Attempt {
    private int failures;
    private Instant lastFailureAt;
    private Instant blockedUntil;
  }
}
