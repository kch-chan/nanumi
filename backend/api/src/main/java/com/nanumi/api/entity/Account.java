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

// 로그인에 쓰는 정보(이메일·비밀번호)를 담음
//
// 리프레시 토큰은 여기 두지 않고 RefreshToken 테이블에 기기별로 둠
// 한 칸만 두면 기기 하나만 로그인을 유지할 수 있었기 때문임 (V2 마이그레이션 참고)
@Entity
@Table(
    name = "accounts",
    uniqueConstraints = {@UniqueConstraint(name = "uk_accounts_email", columnNames = "email")})
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

  // 비밀번호 해시 칸의 길이임
  // NanumiPasswordEncoder 가 만드는 문자열이 이 길이를 넘지 않는지 기동할 때 확인함
  // (반복 횟수가 7자리가 되면 84자가 되어 저장에 실패하므로)
  public static final int PASSWORD_LENGTH = 83;

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
  @Column(nullable = true, length = PASSWORD_LENGTH)
  private String password;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Builder
  public Account(User user, String email, String password) {
    this.user = user;
    this.email = email;
    this.password = password;
  }

  public void changePassword(String password) {
    this.password = password;
  }
}
