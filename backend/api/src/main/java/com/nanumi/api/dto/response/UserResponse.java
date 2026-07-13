package com.nanumi.api.dto.response;

import com.nanumi.api.entity.UserEntity;

public record UserResponse(
    Long id, String nickname, String aptName, String dong, String ho, String role) {

  public static UserResponse from(UserEntity user) {
    return new UserResponse(
        user.getId(),
        user.getNickname(),
        user.getAptName(),
        user.getDong(),
        user.getHo(),
        user.getRole().name());
  }
}
