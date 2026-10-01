package com.nanumi.api.dto.request;

import com.nanumi.api.validation.annotation.SafeText;
import com.nanumi.api.validation.annotation.ValidEmail;
import com.nanumi.api.validation.annotation.ValidNickname;
import com.nanumi.api.validation.annotation.ValidPassword;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

// 이메일과 비밀번호는 비어 있는 경우까지 각 검증기가 직접 알려 주므로 @NotBlank 를 따로 붙이지 않음
// 회원이 자유롭게 적는 값에는 @SafeText 를 붙여서 스크립트가 될 만한 입력을 막음
//
// 비밀번호 재확인은 담지 않음. 두 칸이 같은지 보는 것은 오타를 잡는 일이라 화면에서 끝남.
// 서버로 보내면 같은 비밀번호가 두 번 오가고 로그에 남을 자리도 두 곳이 됨
public record SignupRequest(
    @ValidEmail String email,
    @ValidPassword String password,
    @NotBlank(message = "닉네임을 입력해 주세요.") @SafeText @ValidNickname String nickname,
    @NotBlank(message = "아파트명을 입력해 주세요.") @SafeText
        @Size(max = 100, message = "아파트명은 100자 이하여야 합니다.") String aptName,
    @SafeText @Size(max = 20, message = "동은 20자 이하여야 합니다.") String dong,
    @SafeText @Size(max = 20, message = "호는 20자 이하여야 합니다.") String ho,

    // 약관 동의 기록임. 동의를 받았다는 사실의 입증 책임이 사업자에게 있어 반드시 받음
    // 선택 약관의 거부(agreed = false)도 함께 보내야 "물어봤고 거부했다" 가 기록됨
    //
    // @Valid 가 있어야 목록 안쪽 요소의 제약까지 검사됨.
    // 없으면 key/version 이 비어 있어도 그대로 통과함
    @NotEmpty(message = "약관 동의 정보가 필요합니다.") @Valid @Size(max = 20, message = "약관 동의 정보가 올바르지 않습니다.") List<TermsAgreementRequest> agreements) {}
