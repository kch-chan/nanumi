package com.nanumi.api.security;

import com.nanumi.api.entity.User;
import com.nanumi.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 액세스 토큰에 적힌 사람이 지금도 쓸 수 있는 계정인지 확인함
//
// 왜 필요한가: 액세스 토큰은 서명만 맞으면 통과하는 값이라 서버가 도중에 취소할 수 없음.
// 로그아웃이나 탈퇴로 DB 의 리프레시 토큰을 다 지워도, 이미 나가 있는 액세스 토큰은
// 남은 수명(15분)만큼 그대로 먹힘. 탈퇴한 사람이 15분간 보호된 기능을 계속 쓰게 됨
//
// 그래서 요청마다 상태를 한 번 본다. 비용은 인덱스로 한 행을 세는 조회 한 번임
// (자리표시자 같은 캐시를 얹으면 그만큼 취소가 늦어지므로 지금은 매번 확인하는 쪽을 택함)
//
// 이 확인은 "탈퇴/정지" 만 막음. 로그아웃한 액세스 토큰을 그 즉시 막으려면
// 발급한 토큰을 서버가 따로 들고 있어야 해서 별개의 설계가 필요함
@Component
@RequiredArgsConstructor
public class ActiveUserGuard {

  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public boolean isActive(int userId) {
    return userRepository.existsByIdAndStatus(userId, User.Status.ACTIVE);
  }
}
