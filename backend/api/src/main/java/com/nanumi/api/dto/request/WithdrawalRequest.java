package com.nanumi.api.dto.request;

import com.nanumi.api.validation.annotation.SafeText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 여기 비밀번호는 새로 정하는 값이 아니라 본인 확인용이라 형식 검사를 걸지 않음
// 예전 규칙으로 가입한 회원도 탈퇴할 수 있어야 함
public record WithdrawalRequest(
    @NotBlank(message = "비밀번호를 입력해 주세요.") String password,
    @SafeText @Size(max = 255, message = "탈퇴 사유는 255자 이하여야 합니다.") String reason) {}
