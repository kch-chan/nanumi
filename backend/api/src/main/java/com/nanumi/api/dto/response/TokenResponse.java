package com.nanumi.api.dto.response;

// 토큰 재발급 응답임
// 리프레시할 때마다 리프레시 토큰도 새로 내주므로(회전) 둘 다 갈아 끼워야 함
public record TokenResponse(String accessToken, String refreshToken) {

  public static TokenResponse of(String accessToken, String refreshToken) {
    return new TokenResponse(accessToken, refreshToken);
  }
}
