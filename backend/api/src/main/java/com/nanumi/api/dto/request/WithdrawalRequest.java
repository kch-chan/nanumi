package com.nanumi.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WithdrawalRequest(
    @NotBlank(message = "비밀번호를 입력해 주세요.") String password,
    @Size(max = 255, message = "탈퇴 사유는 255자 이하여야 합니다.") String reason) {}
