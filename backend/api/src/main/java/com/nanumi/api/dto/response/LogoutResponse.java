package com.nanumi.api.dto.response;

public record LogoutResponse(String message) {
  public static LogoutResponse of() {
    return new LogoutResponse("로그아웃이 완료되었습니다.");
  }
}
