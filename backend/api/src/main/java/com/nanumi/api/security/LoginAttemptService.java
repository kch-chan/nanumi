package com.nanumi.api.security;

import com.nanumi.api.exception.CustomException;
import com.nanumi.api.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Component;

// 로그인 실패 횟수를 세서 무차별 대입을 늦춤
//
// 이메일별·IP별로 따로 셈
//   이메일만 세면  : IP 하나로 여러 계정을 돌려 가며 찔러 볼 수 있음
//   IP 만 세면     : 여러 IP 에서 한 계정을 노리는 걸 못 막음
//                 아파트는 공유기를 여러 세대가 같이 쓰는 경우가 있어 IP 임계값을 넉넉히 둠
//
// 기록은 메모리에만 있음. 자세한 한계는 AttemptCounter 주석 참고
@Component
public class LoginAttemptService {

  private static final int EMAIL_MAX_ATTEMPTS = 5;
  private static final int IP_MAX_ATTEMPTS = 20;

  private static final Duration BLOCK_DURATION = Duration.ofMinutes(10);
  private static final Duration ATTEMPT_TTL = Duration.ofMinutes(30);
  private static final int MAX_ENTRIES = 10_000;

  private final AttemptCounter emailCounter =
      new AttemptCounter(EMAIL_MAX_ATTEMPTS, BLOCK_DURATION, ATTEMPT_TTL, MAX_ENTRIES);
  private final AttemptCounter ipCounter =
      new AttemptCounter(IP_MAX_ATTEMPTS, BLOCK_DURATION, ATTEMPT_TTL, MAX_ENTRIES);

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
}
