package com.nanumi.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

// 약관에 "탈퇴 후 30일까지" 라고 적어 두었으므로 그 뒤에는 정말 지워져야 함
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("탈퇴 회원 개인정보 파기")
class WithdrawnDataPurgeServiceTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 12, 0);

  @Mock private UserRepository userRepository;
  @Mock private AccountRepository accountRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private DataErasureLogRepository dataErasureLogRepository;

  private WithdrawnDataPurgeService service;

  @BeforeEach
  void setUp() {
    PrivacyProperties privacyProperties = new PrivacyProperties();
    privacyProperties.setWithdrawnRetentionDays(30);
    service =
        new WithdrawnDataPurgeService(
            userRepository,
            accountRepository,
            refreshTokenRepository,
            dataErasureLogRepository,
            privacyProperties);
  }

  private User withdrawnUser(int id, LocalDateTime withdrawnAt) {
    User user = User.builder().nickname("나눔이" + id).aptName("행복아파트").build();
    ReflectionTestUtils.setField(user, "id", id);
    user.withdraw("이유");
    ReflectionTestUtils.setField(user, "withdrawnAt", withdrawnAt);
    return user;
  }

  private Account accountOf(User user, int accountId) {
    Account account =
        Account.builder().user(user).email("nanumi@example.com").password("해시").build();
    ReflectionTestUtils.setField(account, "id", accountId);
    return account;
  }

  // 기한이 지난 사람만 넘어오는지는 이 조건으로 결정됨
  @Test
  @DisplayName("보유 기한이 지난 사람만 찾음")
  void 기준_시각() {
    when(userRepository.findByStatusAndWithdrawnAtBefore(any(), any())).thenReturn(List.of());

    service.purgeExpired(NOW);

    ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(userRepository)
        .findByStatusAndWithdrawnAtBefore(
            org.mockito.ArgumentMatchers.eq(User.Status.WITHDRAWN), cutoff.capture());
    assertThat(cutoff.getValue()).isEqualTo(NOW.minusDays(30));
  }

  // 외래키를 거스르지 않는 순서로 지워야 함 (refresh_tokens -> accounts -> users)
  @Test
  @DisplayName("토큰·계정·회원을 모두 지움")
  void 전부_지움() {
    User user = withdrawnUser(1, NOW.minusDays(31));
    Account account = accountOf(user, 7);
    when(userRepository.findByStatusAndWithdrawnAtBefore(any(), any())).thenReturn(List.of(user));
    when(accountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));

    int erased = service.purgeExpired(NOW);

    assertThat(erased).isEqualTo(1);
    verify(refreshTokenRepository).deleteByAccount_Id(7);
    verify(accountRepository).delete(account);
    verify(userRepository).delete(user);
  }

  // 지웠다는 사실은 남겨야 함. 다만 기록 자체가 개인정보가 되면 안 되므로 번호와 시각만 담음
  @Test
  @DisplayName("파기 기록을 남기되 개인을 알아볼 값은 담지 않음")
  void 파기_기록() {
    LocalDateTime withdrawnAt = NOW.minusDays(31);
    User user = withdrawnUser(1, withdrawnAt);
    when(userRepository.findByStatusAndWithdrawnAtBefore(any(), any())).thenReturn(List.of(user));
    when(accountRepository.findByUser_Id(1)).thenReturn(Optional.of(accountOf(user, 7)));

    service.purgeExpired(NOW);

    ArgumentCaptor<DataErasureLog> captor = ArgumentCaptor.forClass(DataErasureLog.class);
    verify(dataErasureLogRepository).save(captor.capture());
    DataErasureLog log = captor.getValue();
    assertThat(log.getUserId()).isEqualTo(1);
    assertThat(log.getWithdrawnAt()).isEqualTo(withdrawnAt);
    assertThat(log.getErasedAt()).isEqualTo(NOW);
  }

  // 유니크 제약에 걸리면 배치 전체가 실패해서 나머지 사람도 안 지워짐
  @Test
  @DisplayName("이미 기록이 있으면 같은 줄을 또 남기지 않음")
  void 기록_중복_방지() {
    User user = withdrawnUser(1, NOW.minusDays(31));
    when(userRepository.findByStatusAndWithdrawnAtBefore(any(), any())).thenReturn(List.of(user));
    when(accountRepository.findByUser_Id(1)).thenReturn(Optional.of(accountOf(user, 7)));
    when(dataErasureLogRepository.existsByUserId(1)).thenReturn(true);

    service.purgeExpired(NOW);

    verify(dataErasureLogRepository, never()).save(any());
    // 기록이 있어도 행은 지워야 함
    verify(userRepository).delete(user);
  }

  // 계정 없이 회원 행만 남은 경우에도 회원 행은 지워야 함
  @Test
  @DisplayName("계정 행이 없어도 회원 행은 지움")
  void 계정_없음() {
    User user = withdrawnUser(1, NOW.minusDays(31));
    when(userRepository.findByStatusAndWithdrawnAtBefore(any(), any())).thenReturn(List.of(user));
    when(accountRepository.findByUser_Id(1)).thenReturn(Optional.empty());

    service.purgeExpired(NOW);

    verify(userRepository).delete(user);
    verify(refreshTokenRepository, never()).deleteByAccount_Id(anyInt());
  }

  @Test
  @DisplayName("지울 사람이 없으면 아무것도 건드리지 않음")
  void 대상_없음() {
    when(userRepository.findByStatusAndWithdrawnAtBefore(any(), any())).thenReturn(List.of());

    assertThat(service.purgeExpired(NOW)).isZero();

    verify(userRepository, never()).delete(any());
    verify(dataErasureLogRepository, never()).save(any());
  }
}
