package com.nanumi.api.service;

import com.nanumi.api.config.PrivacyProperties;
import com.nanumi.api.entity.Account;
import com.nanumi.api.entity.DataErasureLog;
import com.nanumi.api.entity.User;
import com.nanumi.api.repository.AccountRepository;
import com.nanumi.api.repository.DataErasureLogRepository;
import com.nanumi.api.repository.RefreshTokenRepository;
import com.nanumi.api.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 보유 기한이 지난 탈퇴 회원의 개인정보를 실제로 지움
//
// 왜 필요한가: 탈퇴는 status 를 WITHDRAWN 으로 바꾸는 것뿐이어서 이메일·비밀번호 해시·
// 단지 정보·탈퇴 사유가 계속 남아 있었음. 약관에는 "탈퇴 후 30일까지" 라고 적어 두었으니
// 적어 둔 것과 실제 동작이 달랐던 것임
//
// 지우는 순서는 외래키를 거스르지 않게 자식부터임
//   refresh_tokens -> accounts -> users
// (refresh_tokens 는 accounts 에 ON DELETE CASCADE 로 걸려 있지만, JPA 가 아는 순서로 지워야
//  영속성 컨텍스트와 DB 가 어긋나지 않음)
//
// 지운 뒤에는 data_erasure_logs 에 "몇 번 회원을 언제 지웠다" 만 남김
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
  private final AccountRepository accountRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final DataErasureLogRepository dataErasureLogRepository;
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

  // 지운 회원 수를 돌려줌. 테스트에서 기준 시각을 넣어 부를 수 있게 열어 둠
  @Transactional
  public int purgeExpired(LocalDateTime now) {
    LocalDateTime cutoff = now.minusDays(privacyProperties.getWithdrawnRetentionDays());

    List<User> expired =
        userRepository.findByStatusAndWithdrawnAtBefore(User.Status.WITHDRAWN, cutoff);

    for (User user : expired) {
      erase(user, now);
    }

    return expired.size();
  }

  private void erase(User user, LocalDateTime now) {
    int userId = user.getId();

    Optional<Account> account = accountRepository.findByUser_Id(userId);
    account.ifPresent(
        it -> {
          refreshTokenRepository.deleteByAccount_Id(it.getId());
          accountRepository.delete(it);
        });

    userRepository.delete(user);

    // 같은 회원 번호로 두 줄이 생기는 것을 막음(유니크 제약이 있어 예외가 나면 배치 전체가 실패함)
    if (!dataErasureLogRepository.existsByUserId(userId)) {
      dataErasureLogRepository.save(
          DataErasureLog.builder()
              .userId(userId)
              .withdrawnAt(user.getWithdrawnAt())
              .erasedAt(now)
              .build());
    }
  }
}
