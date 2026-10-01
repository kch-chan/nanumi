package com.nanumi.api.repository;

import com.nanumi.api.entity.RefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

  // 재발급 요청이 들고 온 토큰을 찾음. 없으면 이미 회전돼 버려진 토큰임
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  // 로그아웃·탈퇴에서 계정 단위로 지움
  void deleteByAccount_Id(Integer accountId);

  // 기기 수를 제한하고 만료된 행을 치울 때 씀. 오래된 것부터 옴
  //
  // 만료된 행 정리는 이 목록을 받아 AuthService.pruneRefreshTokens 가 함.
  // 예전에 있던 deleteByExpiresAtBefore 는 부르는 곳이 없어 지웠음.
  // 쿼리 메서드는 선언만으로 스프링이 구현을 만들어 주므로, 안 쓰는 것이 남아 있으면
  // "어딘가에서 정리가 돌고 있다" 고 잘못 읽히게 됨
  List<RefreshToken> findByAccount_IdOrderByCreatedAtAsc(Integer accountId);
}
