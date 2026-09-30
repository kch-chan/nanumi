package com.nanumi.api.repository;

import com.nanumi.api.entity.DataErasureLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataErasureLogRepository extends JpaRepository<DataErasureLog, Integer> {

  boolean existsByUserId(int userId);
}
