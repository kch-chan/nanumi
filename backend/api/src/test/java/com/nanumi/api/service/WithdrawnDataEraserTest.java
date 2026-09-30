package com.nanumi.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanumi.api.entity.Account;
import com.nanumi.api.entity.DataErasureLog;
import com.nanumi.api.entity.User;
import com.nanumi.api.repository.AccountRepository;
import com.nanumi.api.repository.DataErasureLogRepository;
import com.nanumi.api.repository.RefreshTokenRepository;
import com.nanumi.api.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("한 사람의 개인정보 파기")
class WithdrawnDataEraserTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 12, 0);
  private static final LocalDateTime WITHDRAWN_AT = NOW.minusDays(31);

  @Mock private UserRepository userRepository;
  @Mock private AccountRepository accountRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private DataErasureLogRepository dataErasureLogRepository;

  private WithdrawnDataEraser eraser;

  @BeforeEach
  void setUp() {
    eraser =
        new WithdrawnDataEraser(
            userRepository, accountRepository, refreshTokenRepository, dataErasureLogRepository);
  }

  private User withdrawnUser(int id) {
    User user = User.builder().nickname("나눔이" + id).aptName("행복아파트").build();
    ReflectionTestUtils.setField(user, "id", id);
    user.withdraw("이유");
    ReflectionTestUtils.setField(user, "withdrawnAt", WITHDRAWN_AT);
    return user;
  }

  private Account accountOf(User user, int accountId) {
    Account account =
        Account.builder().user(user).email("nanumi@example.com").password("해시").build();
    ReflectionTestUtils.setField(account, "id", accountId);
    return account;
  }

  private void given(User user, Account account) {
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(accountRepository.findByUser_Id(user.getId()))
        .thenReturn(account == null ? Optional.empty() : Optional.of(account));
  }

  // 외래키를 거스르지 않는 순서로 지워야 함 (refresh_tokens -> accounts -> users)
  @Test
  @DisplayName("토큰·계정·회원을 모두 지움")
  void 전부_지움() {
    User user = withdrawnUser(1);
    Account account = accountOf(user, 7);
    given(user, account);

    eraser.erase(1, NOW);

    verify(refreshTokenRepository).deleteByAccount_Id(7);
    verify(accountRepository).delete(account);
    verify(userRepository).delete(user);
  }

  // 지웠다는 사실은 남겨야 함. 다만 기록 자체가 개인정보가 되면 안 되므로 번호와 시각만 담음
  @Test
  @DisplayName("파기 기록을 남기되 개인을 알아볼 값은 담지 않음")
  void 파기_기록() {
    User user = withdrawnUser(1);
    given(user, accountOf(user, 7));

    eraser.erase(1, NOW);

    ArgumentCaptor<DataErasureLog> captor = ArgumentCaptor.forClass(DataErasureLog.class);
    verify(dataErasureLogRepository).save(captor.capture());
    DataErasureLog log = captor.getValue();
    assertThat(log.getUserId()).isEqualTo(1);
    assertThat(log.getWithdrawnAt()).isEqualTo(WITHDRAWN_AT);
    assertThat(log.getErasedAt()).isEqualTo(NOW);
  }

  // 유니크 제약에 걸리면 이 사람이 실패로 처리되어 행이 남음
  @Test
  @DisplayName("이미 기록이 있으면 같은 줄을 또 남기지 않음")
  void 기록_중복_방지() {
    User user = withdrawnUser(1);
    given(user, accountOf(user, 7));
    when(dataErasureLogRepository.existsByUserId(1)).thenReturn(true);

    eraser.erase(1, NOW);

    verify(dataErasureLogRepository, never()).save(any());
    // 기록이 있어도 행은 지워야 함
    verify(userRepository).delete(user);
  }

  @Test
  @DisplayName("계정 행이 없어도 회원 행은 지움")
  void 계정_없음() {
    User user = withdrawnUser(1);
    given(user, null);

    eraser.erase(1, NOW);

    verify(userRepository).delete(user);
    verify(refreshTokenRepository, never()).deleteByAccount_Id(anyInt());
  }

  // 앞선 실행에서 이미 지웠거나, 그 사이에 없어진 경우임
  @Test
  @DisplayName("회원 행이 없으면 아무것도 하지 않음")
  void 회원_없음() {
    when(userRepository.findById(1)).thenReturn(Optional.empty());

    eraser.erase(1, NOW);

    verify(userRepository, never()).delete(any());
    verify(accountRepository, never()).delete(any());
    verify(dataErasureLogRepository, never()).save(any());
  }
}
