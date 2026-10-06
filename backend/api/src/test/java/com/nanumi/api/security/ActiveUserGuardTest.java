package com.nanumi.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanumi.api.entity.User;
import com.nanumi.api.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 액세스 토큰은 서명만 맞으면 통과하는 값이라 서버가 도중에 취소할 수 없음.
// 로그아웃이나 탈퇴로 DB 의 리프레시 토큰을 다 지워도 이미 나가 있는 액세스 토큰은
// 남은 수명(15분)만큼 그대로 먹힘. 그 구멍을 요청마다 상태를 확인해 메우는 것이 이 클래스임
@ExtendWith(MockitoExtension.class)
@DisplayName("계정 상태 확인")
class ActiveUserGuardTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private ActiveUserGuard activeUserGuard;

  @Test
  @DisplayName("ACTIVE 상태면 통과시킴")
  void 활성_계정() {
    when(userRepository.existsByIdAndStatus(1, User.Status.ACTIVE)).thenReturn(true);

    assertThat(activeUserGuard.isActive(1)).isTrue();
  }

  @Test
  @DisplayName("ACTIVE 가 아니면 막음")
  void 탈퇴_계정() {
    when(userRepository.existsByIdAndStatus(1, User.Status.ACTIVE)).thenReturn(false);

    assertThat(activeUserGuard.isActive(1)).isFalse();
  }

  // 조회 조건이 핵심임
  //
  // status 를 빼고 존재 여부만 보면 탈퇴한 회원도 통과함(탈퇴는 행을 지우는 것이 아니라
  // status 를 WITHDRAWN 으로 바꾸는 Soft Delete 이기 때문).
  // 그래서 "있는가" 가 아니라 "ACTIVE 로 있는가" 를 묻는지까지 확인함
  @Test
  @DisplayName("ACTIVE 조건을 붙여서 조회함")
  void 조회_조건() {
    when(userRepository.existsByIdAndStatus(7, User.Status.ACTIVE)).thenReturn(true);

    activeUserGuard.isActive(7);

    verify(userRepository).existsByIdAndStatus(7, User.Status.ACTIVE);
  }

  // 행 전체를 읽지 않고 있는지만 보는 조회임(exists)
  // 요청마다 도는 자리라 비용이 그대로 응답 시간에 더해지기 때문
  @Test
  @DisplayName("회원 번호가 그대로 전달됨")
  void 회원_번호_전달() {
    when(userRepository.existsByIdAndStatus(12345, User.Status.ACTIVE)).thenReturn(true);

    assertThat(activeUserGuard.isActive(12345)).isTrue();

    verify(userRepository).existsByIdAndStatus(12345, User.Status.ACTIVE);
  }
}
