package com.nanumi.api.repository;

import com.nanumi.api.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

  Optional<User> findByNickname(String nickname);

  // 요청마다 부르는 조회임. 행 전체를 읽지 않고 있는지만 봄
  // 탈퇴한 사람의 액세스 토큰이 남은 수명만큼 계속 통하는 것을 막는 데 씀(ActiveUserGuard)
  boolean existsByIdAndStatus(int id, User.Status status);

  // 닉네임 중복 검사는 대소문자를 무시함. abc 와 ABC 가 같이 있으면 사람이 헷갈리기 때문임
  boolean existsByNicknameIgnoreCase(String nickname);

  // 나눔글은 같은 단지 안에서만 보이므로 단지 이름으로 찾는 것만 남겨 둠
  // 동·호로만 찾는 메서드는 단지 구분 없이 전체에서 찾게 되어 쓸 수 없어 지웠음
  List<User> findByAptName(String aptName);

  // 약관에 적어 둔 "탈퇴 후 30일" 이 지난 사람을 찾음. 개인정보 파기 배치가 씀
  List<User> findByStatusAndWithdrawnAtBefore(User.Status status, LocalDateTime cutoff);
}
