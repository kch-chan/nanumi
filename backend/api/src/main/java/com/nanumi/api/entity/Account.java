package com.nanumi.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "accounts")
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, unique = true, length = 100)
  private String email;

  // NanumiPasswordEncoder 가 만드는 해시 길이에 맞춤
  // 접두사·버전·반복 횟수에 salt(22자)와 hash(43자)를 이어 붙여서 기본값 기준 83자가 나옴
  @Column(nullable = true, length = 83)
  private String password;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Column(unique = true, length = 1000)
  private String refreshToken;

  @Column private LocalDateTime expiryDate;

  @Builder
  public Account(User user, String email, String password) {
    this.user = user;
    this.email = email;
    this.password = password;
  }

  public boolean isExpired() {
    return this.expiryDate == null || LocalDateTime.now().isAfter(this.expiryDate);
  }

  public void updateRefreshToken(String refreshToken, LocalDateTime expiryDate) {
    this.refreshToken = refreshToken;
    this.expiryDate = expiryDate;
  }

  public void clearRefreshToken() {
    this.refreshToken = null;
    this.expiryDate = null;
  }

  public void changePassword(String password) {
    this.password = password;
  }
}
