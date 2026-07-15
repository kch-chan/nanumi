package com.nanumi.api.dto.response;

public record LoginResponse(String accessToken, String refreshToken, UserResponse user) {
  public static LoginResponse of(String accessToken, String refreshToken, UserResponse user) {
    return new LoginResponse(accessToken, refreshToken, user);
  }
}
