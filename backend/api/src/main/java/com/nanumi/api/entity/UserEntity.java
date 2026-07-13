package com.nanumi.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "users")
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 20)
  private String nickname;

  @Column(nullable = false, length = 100)
  private String aptName;

  @Column(nullable = true, length = 20)
  private String dong;

  @Column(nullable = true, length = 20)
  private String ho;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role = Role.USER;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Builder
  public UserEntity(String nickname, String aptName, String dong, String ho) {
    this.nickname = nickname;
    this.aptName = aptName;
    this.dong = dong;
    this.ho = ho;
  }

  public enum Role {
    USER,
    ADMIN
  }

  public void changeNickname(String nickname) {
    this.nickname = nickname;
  }

  public void changeApt(String aptName) {
    this.aptName = aptName;
  }

  public void changeDong(String dong) {
    this.dong = dong;
  }

  public void changHo(String ho) {
    this.ho = ho;
  }
}
