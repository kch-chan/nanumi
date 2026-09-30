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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "accounts",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_accounts_email", columnNames = "email"),
      @UniqueConstraint(name = "uk_accounts_refresh_token_hash", columnNames = "refresh_token_hash")
    })
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

  // 리프레시 토큰을 SHA-256 으로 줄여 담는 칸의 길이임 (16진수 64자)
  public static final int REFRESH_TOKEN_HASH_LENGTH = 64;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 100)
  private String email;

  // NanumiPasswordEncoder 가 만드는 해시 길이에 맞춤
  // 접두사·버전·반복 횟수에 salt(22자)와 hash(43자)를 이어 붙여서 기본값 기준 83자
  @Column(nullable = true, length = 83)
  private String password;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  // 리프레시 토큰을 그대로 담지 않고 SHA-256 해시만 담음
  @Column(name = "refresh_token_hash", length = REFRESH_TOKEN_HASH_LENGTH)
  private String refreshTokenHash;

  @Column private LocalDateTime expiryDate;

  @Builder
  public Account(User user, String email, String password) {
    this.user = user;
    this.email = email;
    this.password = password;
  }

  // 담아 둔 리프레시 토큰이 없거나 기한이 지났으면 참임
  public boolean isExpired() {
    return this.expiryDate == null || LocalDateTime.now().isAfter(this.expiryDate);
  }

  public boolean hasRefreshToken() {
    return this.refreshTokenHash != null;
  }

  public void updateRefreshToken(String refreshTokenHash, LocalDateTime expiryDate) {
    this.refreshTokenHash = refreshTokenHash;
    this.expiryDate = expiryDate;
  }

  public void clearRefreshToken() {
    this.refreshTokenHash = null;
    this.expiryDate = null;
  }

  public void changePassword(String password) {
    this.password = password;
  }
}
