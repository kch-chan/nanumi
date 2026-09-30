package com.nanumi.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanumi.api.config.PrivacyProperties;
import com.nanumi.api.entity.User;
import com.nanumi.api.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
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
// 실제로 지우는 부분은 WithdrawnDataEraserTest 에서 봄
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("탈퇴 회원 파기 대상 고르기")
class WithdrawnDataPurgeServiceTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 12, 0);

  @Mock private UserRepository userRepository;
  @Mock private WithdrawnDataEraser withdrawnDataEraser;

  private WithdrawnDataPurgeService service;

  @BeforeEach
  void setUp() {
    PrivacyProperties privacyProperties = new PrivacyProperties();
    privacyProperties.setWithdrawnRetentionDays(30);
    service = new WithdrawnDataPurgeService(userRepository, withdrawnDataEraser, privacyProperties);
  }

  private User withdrawnUser(int id) {
    User user = User.builder().nickname("나눔이" + id).aptName("행복아파트").build();
    ReflectionTestUtils.setField(user, "id", id);
    user.withdraw("이유");
    return user;
  }

  // 기한을 어떻게 세는지가 이 조건 하나로 결정됨
  @Test
  @DisplayName("보유 기한이 지난 사람만 찾음")
  void 기준_시각() {
    when(userRepository.findByStatusAndWithdrawnAtBefore(any(), any())).thenReturn(List.of());

    service.purgeExpired(NOW);

    ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(userRepository)
        .findByStatusAndWithdrawnAtBefore(eq(User.Status.WITHDRAWN), cutoff.capture());
    assertThat(cutoff.getValue()).isEqualTo(NOW.minusDays(30));
  }

  @Test
  @DisplayName("찾은 사람마다 파기를 맡김")
  void 전부_넘김() {
    when(userRepository.findByStatusAndWithdrawnAtBefore(any(), any()))
        .thenReturn(List.of(withdrawnUser(1), withdrawnUser(2)));

    assertThat(service.purgeExpired(NOW)).isEqualTo(2);

    verify(withdrawnDataEraser).erase(1, NOW);
    verify(withdrawnDataEraser).erase(2, NOW);
  }

  // 한 행 때문에 아무도 파기되지 않으면 안 됨
  @Test
  @DisplayName("한 사람에서 실패해도 나머지는 지움")
  void 하나_실패() {
    when(userRepository.findByStatusAndWithdrawnAtBefore(any(), any()))
        .thenReturn(List.of(withdrawnUser(1), withdrawnUser(2), withdrawnUser(3)));
    doThrow(new RuntimeException("DB 오류")).when(withdrawnDataEraser).erase(2, NOW);

    assertThat(service.purgeExpired(NOW)).isEqualTo(2);

    verify(withdrawnDataEraser).erase(1, NOW);
    verify(withdrawnDataEraser).erase(3, NOW);
  }

  @Test
  @DisplayName("지울 사람이 없으면 아무것도 맡기지 않음")
  void 대상_없음() {
    when(userRepository.findByStatusAndWithdrawnAtBefore(any(), any())).thenReturn(List.of());

    assertThat(service.purgeExpired(NOW)).isZero();

    verify(withdrawnDataEraser, never()).erase(anyInt(), any());
  }
}
