package com.nanumi.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 토큰 재발급 요청임
// JWT 는 길어야 1KB 안쪽이므로 상한을 넉넉히 두되, 무제한으로 받지는 않음
public record RefreshRequest(
    @NotBlank(message = "리프레시 토큰을 입력해 주세요.") @Size(max = 2000, message = "리프레시 토큰이 올바르지 않습니다.") String refreshToken) {}
