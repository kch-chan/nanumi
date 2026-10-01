package com.nanumi.api.service;

import com.nanumi.api.entity.DataErasureLog;
import com.nanumi.api.entity.User;
import com.nanumi.api.repository.AccountRepository;
import com.nanumi.api.repository.DataErasureLogRepository;
import com.nanumi.api.repository.RefreshTokenRepository;
import com.nanumi.api.repository.TermsAgreementRepository;
import com.nanumi.api.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 한 사람의 개인정보를 지움. 한 사람이 한 트랜잭션임
//
// 왜 클래스를 따로 두는가: 트랜잭션은 빈 바깥에서 불릴 때만 걸림.
// 같은 클래스 안에서 메서드를 부르면(자기 호출) 프록시를 타지 않아 @Transactional 이 조용히 무시됨.
// 그래서 "돌릴 대상을 고르는 일"(WithdrawnDataPurgeService)과 "한 사람을 지우는 일"을 나눔
//
// 사람마다 트랜잭션을 끊는 이유: 한 사람에서 실패해도 나머지 사람은 지워져야 함.
// 전체를 한 트랜잭션으로 묶으면 한 행 때문에 아무도 파기되지 않음
@Component
@RequiredArgsConstructor
public class WithdrawnDataEraser {

  private final UserRepository userRepository;
  private final AccountRepository accountRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final TermsAgreementRepository termsAgreementRepository;
  private final DataErasureLogRepository dataErasureLogRepository;

  // 지우는 순서는 외래키를 거스르지 않게 자식부터임
  //   refresh_tokens -> accounts -> terms_agreements -> users
  // (자식 테이블은 ON DELETE CASCADE 로 걸려 있지만,
  //  JPA 가 아는 순서로 지워야 영속성 컨텍스트와 DB 가 어긋나지 않음)
  //
  // REQUIRES_NEW 인 이유: 부르는 쪽이 읽기 전용 트랜잭션을 열어 둔 상태라면 REQUIRED 는
  // 그 트랜잭션에 참여해 버리고, 읽기 전용에서는 flush 가 생략되어 delete 가 조용히 사라짐.
  // 사람마다 트랜잭션을 끊는다는 이 클래스의 전제를 부르는 쪽 사정과 무관하게 보장함
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void erase(int userId, LocalDateTime now) {
    Optional<User> found = userRepository.findById(userId);
    if (found.isEmpty()) {
      // 앞선 실행에서 이미 지웠음
      return;
    }
    User user = found.get();

    accountRepository
        .findByUser_Id(userId)
        .ifPresent(
            account -> {
              refreshTokenRepository.deleteByAccount_Id(account.getId());
              accountRepository.delete(account);
            });

    // 약관 동의 기록도 개인정보에 딸린 자료이므로 같은 기한에 함께 지움
    // (동의 사실 자체는 data_erasure_logs 가 "언제 파기했는지"로 남음)
    termsAgreementRepository.deleteByUser_Id(userId);

    LocalDateTime withdrawnAt = user.getWithdrawnAt();
    userRepository.delete(user);

    // 같은 회원 번호로 두 줄이 생기는 것을 막음(유니크 제약이 있어 예외가 나면 이 사람이 실패로 처리됨)
    if (!dataErasureLogRepository.existsByUserId(userId)) {
      dataErasureLogRepository.save(
          DataErasureLog.builder().userId(userId).withdrawnAt(withdrawnAt).erasedAt(now).build());
    }
  }
}
