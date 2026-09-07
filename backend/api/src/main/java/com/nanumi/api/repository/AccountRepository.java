package com.nanumi.api.repository;

import com.nanumi.api.entity.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// 비밀번호 해시로 계정을 찾는 메서드는 두지 않음
// 해시가 같은 계정을 되짚을 수 있으면 같은 비밀번호를 쓰는 사람들을 한꺼번에 찾아낼 수 있기 때문임
public interface AccountRepository extends JpaRepository<Account, Long> {

  Optional<Account> findByEmail(String email);

  Optional<Account> findByUser_Id(Long userId);

  boolean existsByEmail(String email);
}
