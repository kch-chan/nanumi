package com.nanumi.api.service;

import com.nanumi.api.config.PrivacyProperties;
import com.nanumi.api.entity.User;
import com.nanumi.api.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 보유 기한이 지난 탈퇴 회원을 찾아 WithdrawnDataEraser 에게 넘김
//
// 왜 필요한가: 탈퇴는 status 를 WITHDRAWN 으로 바꾸는 것뿐이어서 이메일·비밀번호 해시·
// 단지 정보·탈퇴 사유가 계속 남아 있었음. 약관에는 "탈퇴 후 30일까지" 라고 적어 두었으니
// 적어 둔 것과 실제 동작이 달랐던 것임
//
// 실제로 지우는 일은 WithdrawnDataEraser 가 함. 사람마다 트랜잭션을 끊어야 하고,
// 같은 클래스 안에서 부르면 프록시를 타지 않아 트랜잭션이 걸리지 않기 때문임
//
// 도는 시점: 하루 한 번 같은 고정 시각이 아니라 일정 간격임.
// Render 무료 등급은 15분 요청이 없으면 인스턴스를 멈추므로, 새벽 4시 같은 고정 시각을 잡으면
// 그 시각에 잠들어 있어서 한 번도 안 돌 수 있음. 기동 뒤 조금 있다가, 그다음에는 간격마다 돌게 둠
//
// 그래도 인스턴스가 오래 잠들어 있으면 파기가 늦어짐.
// 기한을 반드시 지켜야 하는 단계가 되면 바깥에서 주기적으로 깨우거나
// 플랫폼의 예약 작업(Cron Job)으로 옮겨야 함. docs/deployment.md 에 적어 둠
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawnDataPurgeService {

  private final UserRepository userRepository;
  private final WithdrawnDataEraser withdrawnDataEraser;
  private final PrivacyProperties privacyProperties;

  @Scheduled(
      initialDelayString = "${nanumi.privacy.purge-initial-delay:PT2M}",
      fixedDelayString = "${nanumi.privacy.purge-interval:PT6H}")
  public void purgeOnSchedule() {
    int erased = purgeExpired(LocalDateTime.now());
    if (erased > 0) {
      log.info("보유 기한이 지난 탈퇴 회원 {}명의 개인정보를 파기함", erased);
    }
  }

  // 지운 사람 수를 돌려줌. 테스트에서 기준 시각을 넣어 부를 수 있게 열어 둠
  //
  // 대상을 고르는 조회만 트랜잭션에 넣음. 지우는 일은 사람마다 따로 커밋됨
  @Transactional(readOnly = true)
  public int purgeExpired(LocalDateTime now) {
    LocalDateTime cutoff = now.minusDays(privacyProperties.getWithdrawnRetentionDays());

    List<User> expired =
        userRepository.findByStatusAndWithdrawnAtBefore(User.Status.WITHDRAWN, cutoff);

    int erased = 0;
    for (User user : expired) {
      // 한 사람에서 실패해도 나머지 사람은 지워져야 함
      try {
        withdrawnDataEraser.erase(user.getId(), now);
        erased++;
      } catch (RuntimeException e) {
        log.error("회원 {} 의 개인정보를 지우지 못함. 다음 실행에서 다시 시도함", user.getId(), e);
      }
    }

    return erased;
  }
}
