package com.nanumi.api.repository;

import com.nanumi.api.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginEntityRepository extends JpaRepository<UserEntity, Long> {}
