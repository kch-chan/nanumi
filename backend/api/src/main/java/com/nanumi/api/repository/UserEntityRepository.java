package com.nanumi.api.repository;

import com.nanumi.api.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;



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
