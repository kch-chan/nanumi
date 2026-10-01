package com.nanumi.api.dto.request;

import com.nanumi.api.validation.annotation.SafeText;
import com.nanumi.api.validation.validator.PasswordValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 여기 비밀번호는 새로 정하는 값이 아니라 본인 확인용이라 형식 검사를 걸지 않음
// 예전 규칙으로 가입한 회원도 탈퇴할 수 있어야 함
//
// 다만 길이 상한은 LoginRequest 와 같이 둠
// 상한이 없으면 수 MB 짜리 문자열 하나로 유니코드 정규화와 PBKDF2 10만 회를 그대로 돌리게 됨.
// 탈퇴는 인증이 필요한 경로지만, 로그인한 계정 하나로도 서버를 붙잡아 둘 수 있음
public record WithdrawalRequest(
    @NotBlank(message = "비밀번호를 입력해 주세요.") @Size(max = PasswordValidator.MAX_LENGTH, message = "비밀번호는 20자 이하여야 합니다.") String password,
    @SafeText @Size(max = 255, message = "탈퇴 사유는 255자 이하여야 합니다.") String reason) {}
