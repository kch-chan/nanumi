package com.nanumi.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 로그인은 형식을 자세히 따지지 않음
// 예전 규칙으로 가입한 회원도 로그인은 되어야 하고, 형식을 알려 주면 계정 탐색에 힌트가 되기 때문임
//
// 다만 길이 상한은 둠
// 상한이 없으면 수 MB 짜리 문자열을 보내는 것만으로 유니코드 정규화와 PBKDF2 를 그대로 돌리게 됨
public record LoginRequest(
    @NotBlank(message = "이메일을 입력해 주세요.") @Size(max = 100, message = "이메일은 100자 이하여야 합니다.") String email,
    @NotBlank(message = "비밀번호를 입력해 주세요.") @Size(max = 200, message = "비밀번호는 200자 이하여야 합니다.") String password) {}
