package com.nanumi.api.dto.request;

import com.nanumi.api.validation.annotation.ValidNickname;
import com.nanumi.api.validation.annotation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank @Email(message = "올바른 이메일 형식이 아닙니다.") String email,
    @NotBlank(message = "비밀번호를 입력해 주세요.") @ValidPassword String password,

    // 비밀번호 재확인 변수도 나중에 생성

    @NotBlank(message = "닉네임을 입력해 주세요.") @ValidNickname String nickname,
    @NotBlank(message = "아파트명을 입력해 주세요.") @Size(max = 100, message = "아파트명은 100자 이하여야 합니다.") String aptName,
    @Size(max = 20, message = "동은 20자 이하여야 합니다.") String dong,
    @Size(max = 20, message = "호는 20자 이하여야 합니다.") String ho) {}
