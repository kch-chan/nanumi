package com.nanumi.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

// 기기 하나가 로그인을 유지하는 데 쓰는 리프레시 토큰 한 개임
//
// 계정 하나에 여러 행이 있을 수 있음(기기마다 하나).
// 그래서 PC 에서 로그인해도 폰의 로그인이 끊기지 않음
@Entity
@Table(
    name = "refresh_tokens",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_refresh_tokens_token_hash", columnNames = "token_hash")
    },
    indexes = {@Index(name = "ix_refresh_tokens_account_id", columnList = "account_id")})
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

  // 리프레시 토큰을 SHA-256 으로 줄여 담는 칸의 길이임 (16진수 64자)
  public static final int TOKEN_HASH_LENGTH = 64;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  // 토큰 원문이 아니라 해시만 담음
  // 원문을 담아 두면 DB 가 유출됐을 때 그대로 로그인에 쓸 수 있는 자격 증명이 됨
  @Column(name = "token_hash", nullable = false, length = TOKEN_HASH_LENGTH)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Builder
  public RefreshToken(Account account, String tokenHash, LocalDateTime expiresAt) {
    this.account = account;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(this.expiresAt);
  }
}
