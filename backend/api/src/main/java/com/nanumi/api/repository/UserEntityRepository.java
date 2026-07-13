package com.nanumi.api.repository;

import com.nanumi.api.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEntityRepository extends JpaRepository<UserEntity, Long> {
  Optional<UserEntity> findByNickname(String nickname);

  List<UserEntity> findByAptName(String aptName);

  List<UserEntity> findByDong(String dong);

  List<UserEntity> findByHo(String ho);

  boolean existsByNickname(String nickname);

  boolean existsByAptName(String aptName);

  boolean existsByDong(String dong);

  boolean existsByHo(String ho);
}
