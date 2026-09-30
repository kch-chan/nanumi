package com.nanumi.api.dto.response;

public record SignupResponse(String message, UserResponse user) {
  public static SignupResponse of(UserResponse user) {
    return new SignupResponse("회원가입에 성공하였습니다", user);
  }
}
