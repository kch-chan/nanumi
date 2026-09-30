package com.nanumi.api.security;

import com.nanumi.api.exception.CustomException;
import com.nanumi.api.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

// 로그인 무차별 대입을 막는 카운터임
//
// 이메일과 IP 를 한 키로 묶으면 IP 만 바꿔도 카운터가 새로 시작해서 정작 막고 싶던 공격을 못 막음
// 그래서 카운터를 둘로 나누고 둘 중 하나라도 걸리면 차단함
//   - 이메일 단위: 계정 하나를 노리고 비밀번호를 바꿔 가며 찔러 보는 것을 막음. 임계값을 낮게 둠
//   - IP 단위   : 한 회선에서 계정을 바꿔 가며 찔러 보는 것을 막음
//                 아파트는 공유기를 여러 세대가 같이 쓰는 경우가 있어 임계값을 넉넉히 둠
//
// 메모리에만 들고 있어서 서버를 여러 대로 늘리면 인스턴스별로 따로 셈
// 그때는 Redis 같은 공용 저장소로 옮겨야 함
@Component
public class LoginAttemptService {

  private static final int EMAIL_MAX_ATTEMPTS = 5;
  private static final int IP_MAX_ATTEMPTS = 20;

  private static final Duration BLOCK_DURATION = Duration.ofMinutes(10);

  // 실패한 채로 한참 지난 기록은 처음부터 다시 셈
  private static final Duration ATTEMPT_TTL = Duration.ofMinutes(30);

  // 카운터 하나가 들고 있을 수 있는 최대 항목 수임
  // 상한이 없으면 키를 계속 바꿔 가며 요청하는 것만으로 메모리를 고갈시킬 수 있음
  // 넘치면 가장 오래 안 쓴 항목부터 밀어냄
  private static final int MAX_ENTRIES = 10_000;

  private final Counter emailCounter = new Counter(EMAIL_MAX_ATTEMPTS);
  private final Counter ipCounter = new Counter(IP_MAX_ATTEMPTS);
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
    Instant now = clock.instant();

    // 둘 다 확인해야 지나간 기록이 양쪽 모두 정리되므로 단축 평가를 쓰지 않음
    boolean emailBlocked = emailCounter.isBlocked(normalize(email), now);
    boolean ipBlocked = ipCounter.isBlocked(normalize(clientIp), now);

    return emailBlocked || ipBlocked;
  }

  public void recordFailure(String email, String clientIp) {
    Instant now = clock.instant();

    emailCounter.recordFailure(normalize(email), now);
    ipCounter.recordFailure(normalize(clientIp), now);
  }

  // 로그인에 성공하면 그 계정의 실패 기록은 지움
  // IP 쪽은 남겨 둠. 계정 하나만 제대로 맞히고 나머지를 계속 찔러 보는 걸 막아야 하기 때문임
  public void recordSuccess(String email, String clientIp) {
    emailCounter.clear(normalize(email));
  }

  // 그 계정에 남은 시도 횟수임. 이미 막혀 있으면 0 임
  public int getRemainingAttempts(String email, String clientIp) {
    return emailCounter.remaining(normalize(email), clock.instant());
  }

  // 이메일은 대소문자를 가리지 않으므로 소문자로 맞춰서 셈
  private String normalize(String key) {
    return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
  }

  private static final class Counter {

    private final int maxAttempts;
    private final Map<String, Attempt> attempts = createBoundedMap();

    private Counter(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    private boolean isBlocked(String key, Instant now) {
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

    private void recordFailure(String key, Instant now) {
      attempts.compute(
          key,
          (ignored, current) -> {
            Attempt attempt = (current == null || isStale(current, now)) ? new Attempt() : current;
            attempt.failures++;
            attempt.lastFailureAt = now;
            if (attempt.failures >= maxAttempts) {
              attempt.blockedUntil = now.plus(BLOCK_DURATION);
            }
            return attempt;
          });
    }

    private void clear(String key) {
      attempts.remove(key);
    }

    private int remaining(String key, Instant now) {
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
      return attempt.lastFailureAt == null
          || !now.isBefore(attempt.lastFailureAt.plus(ATTEMPT_TTL));
    }

    // 접근 순서로 정렬되는 지도라서, 상한을 넘으면 가장 오래 안 쓴 항목이 먼저 밀려남
    // 밀려난 자리에 차단 기록이 있었다면 그 키는 다시 처음부터 세게 됨
    // 메모리가 무한정 늘어나는 것보다는 낫다고 보고 감수함
    private static Map<String, Attempt> createBoundedMap() {
      return Collections.synchronizedMap(
          new LinkedHashMap<String, Attempt>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Attempt> eldest) {
              return size() > MAX_ENTRIES;
            }
          });
    }
  }

  private static final class Attempt {
    private int failures;
    private Instant lastFailureAt;
    private Instant blockedUntil;
  }
}
