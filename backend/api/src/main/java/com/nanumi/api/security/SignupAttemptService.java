package com.nanumi.api.security;

import com.nanumi.api.exception.CustomException;
import com.nanumi.api.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import org.springframework.stereotype.Component;

// 한 IP 에서 가입을 몇 번까지 할 수 있는지 제한함
//
// 가입 경로는 인증이 필요 없어서(permitAll) 제한이 없으면 계정을 대량으로 만들 수 있음.
// 그리고 이미 있는 이메일이면 409 를 돌려주므로, 그것만 보고 "이 이메일이 가입돼 있는지"를
// 훑을 수도 있음. 그래서 성공·실패를 가리지 않고 시도 자체를 셈
//
// 아파트는 공유기를 여러 세대가 같이 쓸 수 있어 한 시간에 5번으로 잡음.
// 한 가구가 한 시간에 여섯 번 가입할 일은 없고, 있어도 한 시간 뒤에 다시 됨
//
// 기록은 메모리에만 있음. 자세한 한계는 AttemptCounter 주석 참고
@Component
public class SignupAttemptService {

  static final int IP_MAX_ATTEMPTS = 5;

  private static final Duration BLOCK_DURATION = Duration.ofHours(1);
  private static final Duration ATTEMPT_TTL = Duration.ofHours(1);
  private static final int MAX_ENTRIES = 10_000;

  private final AttemptCounter ipCounter =
      new AttemptCounter(IP_MAX_ATTEMPTS, BLOCK_DURATION, ATTEMPT_TTL, MAX_ENTRIES);

  private final Clock clock;

  public SignupAttemptService() {
    this(Clock.systemDefaultZone());
  }

  // 테스트에서 시간을 직접 흘려보내려고 열어 둔 생성자임
  SignupAttemptService(Clock clock) {
    this.clock = clock;
  }

  // 막혀 있으면 예외를 던짐. 가입 처리 맨 앞에서 부름
  public void checkBlocked(String clientIp) {
    if (ipCounter.isBlocked(normalize(clientIp), clock.instant())) {
      throw new CustomException(ErrorCode.SIGNUP_ATTEMPT_EXCEEDED);
    }
  }

  // 성공하든 실패하든 시도한 것으로 셈
  public void recordAttempt(String clientIp) {
    ipCounter.recordFailure(normalize(clientIp), clock.instant());
  }

  public int getRemainingAttempts(String clientIp) {
    return ipCounter.remaining(normalize(clientIp), clock.instant());
  }

  private String normalize(String key) {
    return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
  }
}
