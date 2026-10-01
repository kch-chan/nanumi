package com.nanumi.api.dto.request;

import com.nanumi.api.entity.TermsAgreement;
import com.nanumi.api.validation.annotation.SafeText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 가입할 때 함께 보내는 약관 동의 한 건임
//
// key 를 서버가 정한 목록과 맞춰 보는 일은 AuthService 에서 함
// (여기서 @Pattern 으로 묶어 두면 약관을 추가할 때마다 정규식을 고쳐야 하고,
//  "모르는 key 를 보냈다" 와 "필수 약관을 빠뜨렸다" 를 구분해서 알려 주기 어려움)
public record TermsAgreementRequest(
    @NotBlank(message = "약관 종류가 필요합니다.") @Size(max = TermsAgreement.TERMS_KEY_LENGTH, message = "약관 종류가 올바르지 않습니다.") @SafeText
        String key,
    @NotBlank(message = "약관 판이 필요합니다.") @Size(max = TermsAgreement.TERMS_VERSION_LENGTH, message = "약관 판이 올바르지 않습니다.") @SafeText
        String version,
    // 선택 약관은 false 도 기록 대상이므로 "안 보냄" 과 "거부" 를 구분함
    // 원시 타입 boolean 이면 null 이 조용히 false 가 되어 둘을 구분할 수 없음
    Boolean agreed) {

  public boolean isAgreed() {
    return Boolean.TRUE.equals(agreed);
  }
}
