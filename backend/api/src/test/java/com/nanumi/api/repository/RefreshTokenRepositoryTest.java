package com.nanumi.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nanumi.api.entity.Account;
import com.nanumi.api.entity.RefreshToken;
import com.nanumi.api.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

// 기기 하나가 한 행임. 그래서 PC 에서 로그인해도 폰의 로그인이 끊기지 않음
//
// 메서드 이름으로 만들어지는 쿼리는 이름이 한 글자만 틀려도 앱을 띄울 때 터짐.
// DB 는 실제 PostgreSQL 이라 db/migration 의 제약(UNIQUE·FK)이 실제로 걸리는지까지 여기서 확인됨
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("리프레시 토큰 저장소")
class RefreshTokenRepositoryTest {

  @Autowired private UserRepository userRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;

  private Account account;

  @BeforeEach
  void setUp() {
    User user = userRepository.save(User.builder().nickname("나눔이").aptName("행복아파트").build());
    account =
        accountRepository.save(
            Account.builder()
                .user(user)
                .email("nanumi@example.com")
                .password("$nanumi$1$1000$s$h")
                .build());
  }

  private RefreshToken save(String hash, LocalDateTime expiresAt) {
    return refreshTokenRepository.save(
        RefreshToken.builder().account(account).tokenHash(hash).expiresAt(expiresAt).build());
  }

  private RefreshToken save(String hash) {
    return save(hash, LocalDateTime.now().plusDays(14));
  }

  @Test
  @DisplayName("해시로 토큰 행을 찾음")
  void 해시로_찾기() {
    save("a".repeat(64));

    assertThat(refreshTokenRepository.findByTokenHash("a".repeat(64))).isPresent();
    assertThat(refreshTokenRepository.findByTokenHash("b".repeat(64))).isEmpty();
  }

  // 재발급에서 이 조회가 비면 "이미 회전되어 버려진 토큰" 으로 보고 재사용으로 판정함.
  // 그래서 "없으면 빈 값" 이 정상 동작이고, 예외가 나면 안 됨
  @Test
  @DisplayName("없는 해시를 찾아도 예외가 나지 않음")
  void 없는_해시() {
    assertThat(refreshTokenRepository.findByTokenHash("없는값")).isEmpty();
  }

  @Test
  @DisplayName("한 계정에 여러 기기의 행을 둘 수 있음")
  void 기기별_행() {
    save("a".repeat(64));
    save("b".repeat(64));

    assertThat(refreshTokenRepository.findByAccount_IdOrderByCreatedAtAsc(account.getId()))
        .hasSize(2);
  }

  // 기기 수 상한을 넘었을 때 "오래된 것부터" 지우려면 이 정렬이 맞아야 함
  @Test
  @DisplayName("계정의 토큰을 오래된 순으로 가져옴")
  void 오래된_순() {
    save("a".repeat(64));
    save("b".repeat(64));
    save("c".repeat(64));

    List<RefreshToken> tokens =
        refreshTokenRepository.findByAccount_IdOrderByCreatedAtAsc(account.getId());

    assertThat(tokens).hasSize(3);
    assertThat(tokens)
        .isSortedAccordingTo((left, right) -> left.getCreatedAt().compareTo(right.getCreatedAt()));
  }

  // 로그아웃(전 기기)·탈퇴·재사용 감지가 모두 이 메서드를 씀
  @Test
  @DisplayName("계정 단위로 전부 지움")
  void 계정_단위_삭제() {
    save("a".repeat(64));
    save("b".repeat(64));

    refreshTokenRepository.deleteByAccount_Id(account.getId());

    assertThat(refreshTokenRepository.findByAccount_IdOrderByCreatedAtAsc(account.getId()))
        .isEmpty();
  }

  @Test
  @DisplayName("만료된 행도 그대로 조회됨")
  void 만료_행도_조회됨() {
    save("a".repeat(64), LocalDateTime.now().minusDays(1));

    List<RefreshToken> tokens =
        refreshTokenRepository.findByAccount_IdOrderByCreatedAtAsc(account.getId());

    // 치우는 판단은 저장소가 아니라 AuthService.pruneRefreshTokens 가 함
    assertThat(tokens).hasSize(1);
    assertThat(tokens.get(0).isExpired()).isTrue();
  }

  // 하나의 토큰이 두 계정을 인증하는 상태를 DB 차원에서 막음
  @Test
  @DisplayName("같은 해시를 두 번 담을 수 없음")
  void 해시_중복_불가() {
    save("a".repeat(64));

    assertThatThrownBy(
            () ->
                refreshTokenRepository.saveAndFlush(
                    RefreshToken.builder()
                        .account(account)
                        .tokenHash("a".repeat(64))
                        .expiresAt(LocalDateTime.now().plusDays(14))
                        .build()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
