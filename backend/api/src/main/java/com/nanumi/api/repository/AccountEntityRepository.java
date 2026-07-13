package com.nanumi.api.repository;

import com.nanumi.api.entity.AccountEntity;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountEntityRepository extends JpaRepository<AccountEntity, Long> {
  Optional<AccountEntity> findByEmail(String email);
  Optional<AccountEntity> findByPassword(String password);

  boolean existsByEmail(String email);
  boolean existsByPassword(String password);

}
