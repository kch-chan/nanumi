package com.nanumi.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 개인정보를 파기했다는 기록 한 줄임
//
// 약관에 "탈퇴 후 30일이 지나면 파기한다" 고 적어 두었으므로, 정말 지웠는지 보여 줄 수 있어야 함.
// 다만 기록 자체가 개인정보가 되면 안 되므로 회원 번호와 시각만 남김.
// 그 번호로 되짚을 수 있는 users/accounts 행은 이미 없음
@Entity
@Table(
    name = "data_erasure_logs",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_data_erasure_logs_user_id", columnNames = "user_id"))
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DataErasureLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  // 지워진 users.id. 외래키를 걸지 않음. 가리킬 행이 이미 사라졌기 때문임
  @Column(name = "user_id", nullable = false)
  private int userId;

  @Column(name = "withdrawn_at", nullable = false)
  private LocalDateTime withdrawnAt;

  @Column(name = "erased_at", nullable = false)
  private LocalDateTime erasedAt;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Builder
  public DataErasureLog(int userId, LocalDateTime withdrawnAt, LocalDateTime erasedAt) {
    this.userId = userId;
    this.withdrawnAt = withdrawnAt;
    this.erasedAt = erasedAt;
  }
}
