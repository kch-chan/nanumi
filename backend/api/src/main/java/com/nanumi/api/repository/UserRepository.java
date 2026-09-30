package com.nanumi.api.repository;

import com.nanumi.api.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

  Optional<User> findByNickname(String nickname);

  // 닉네임 중복 검사는 대소문자를 무시함. abc 와 ABC 가 같이 있으면 사람이 헷갈리기 때문임
  boolean existsByNicknameIgnoreCase(String nickname);

  // 나눔글은 같은 단지 안에서만 보이므로 단지 이름으로 찾는 것만 남겨 둠
  // 동·호로만 찾는 메서드는 단지 구분 없이 전체에서 찾게 되어 쓸 수 없어 지웠음
  List<User> findByAptName(String aptName);
}
