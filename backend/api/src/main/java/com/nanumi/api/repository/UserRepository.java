package com.nanumi.api.repository;

import com.nanumi.api.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByNickname(String nickname);

  List<User> findByAptName(String aptName);

  List<User> findByDong(String dong);

  List<User> findByHo(String ho);

  boolean existsByNickname(String nickname);

  boolean existsByAptName(String aptName);

  boolean existsByDong(String dong);

  boolean existsByHo(String ho);
}
