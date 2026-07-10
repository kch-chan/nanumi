package com.nanumi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nanumi.api.entity.UserEntity;

public interface UserEntityRepository extends JpaRepository<UserEntity, Long> {
    
}
