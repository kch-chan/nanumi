package com.nanumi.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class UserEntity {
  private Long id;
  private String email;
  private String password;
  private String nickname;
  private String aptName;
  private String dong;
  private String ho;

  @Builder
  public UserEntity(String email, String password, String nickname, String aptName, String dong, String ho) {
    this.email = email;
    this.password = password;
    this.nickname = nickname;
    this.aptName = aptName;
    this.dong = dong;
    this.ho = ho;
  }
}
