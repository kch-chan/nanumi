package com.nanumi.api.repository;

import com.nanumi.api.entity.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
  Optional<Account> findByEmail(String email);

  Optional<Account> findByPassword(String password);

  boolean existsByEmail(String email);

  boolean existsByPassword(String password);
}
