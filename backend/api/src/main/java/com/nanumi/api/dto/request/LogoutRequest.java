package com.nanumi.api.dto.request;

import jakarta.validation.constraints.Size;

// 로그아웃할 때 어느 기기를 끊을지 알려 주는 몸통임
//
// 리프레시 토큰을 담아 보내면 그 기기만 끊음. 비워 두거나 몸통을 아예 보내지 않으면
// 이 계정의 모든 기기를 끊음(토큰을 잃어버렸을 때를 위한 길)
public record LogoutRequest(@Size(max = 2000, message = "토큰 형식이 올바르지 않습니다.") String refreshToken) {}
