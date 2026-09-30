package com.nanumi.api.repository;

import com.nanumi.api.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

  // 재발급 요청이 들고 온 토큰을 찾음. 없으면 이미 회전돼 버려진 토큰임
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  // 로그아웃·탈퇴에서 계정 단위로 지움
  void deleteByAccount_Id(Integer accountId);

  // 기기 수를 제한하고 만료된 행을 치울 때 씀. 오래된 것부터 옴
  List<RefreshToken> findByAccount_IdOrderByCreatedAtAsc(Integer accountId);

  // 기한이 지난 행을 정리함
  void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
