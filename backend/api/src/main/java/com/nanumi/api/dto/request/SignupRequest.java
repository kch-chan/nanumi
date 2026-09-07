package com.nanumi.api.dto.request;

import com.nanumi.api.validation.annotation.SafeText;
import com.nanumi.api.validation.annotation.ValidEmail;
import com.nanumi.api.validation.annotation.ValidNickname;
import com.nanumi.api.validation.annotation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 이메일과 비밀번호는 비어 있는 경우까지 각 검증기가 직접 알려 주므로 @NotBlank 를 따로 붙이지 않음
// 회원이 자유롭게 적는 값에는 @SafeText 를 붙여서 스크립트가 될 만한 입력을 막음
public record SignupRequest(
    @ValidEmail String email,
    @ValidPassword String password,

    // 비밀번호 재확인 변수도 나중에 만들어야 함

    @NotBlank(message = "닉네임을 입력해 주세요.") @SafeText @ValidNickname String nickname,
    @NotBlank(message = "아파트명을 입력해 주세요.") @SafeText
        @Size(max = 100, message = "아파트명은 100자 이하여야 합니다.") String aptName,
    @SafeText @Size(max = 20, message = "동은 20자 이하여야 합니다.") String dong,
    @SafeText @Size(max = 20, message = "호는 20자 이하여야 합니다.") String ho) {}
