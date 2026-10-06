package com.nanumi.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("개인정보 보유 기한 설정값")
class PrivacyPropertiesTest {

  // 이 값은 화면에 보여 주는 약관과 같아야 함
  //
  // frontend/src/constants/terms.ts 의 개인정보 수집·이용 동의에 "회원 탈퇴 후 30일까지 보유한 뒤
  // 파기한다" 고 적어 두었음. 한쪽만 고치면 적어 둔 것과 실제 동작이 달라지는데, 그건 기능 결함이
  // 아니라 약관 위반이 됨. 그래서 기본값을 테스트로 못 박아 두고, 바꿀 때 약관도 함께 보게 함
  @Test
  @DisplayName("보유 기한의 기본값은 30일임")
  void 기본값() {
    assertThat(new PrivacyProperties().getWithdrawnRetentionDays()).isEqualTo(30);
  }

  @Test
  @DisplayName("설정으로 값을 덮어쓸 수 있음")
  void 값_덮어쓰기() {
    PrivacyProperties properties = new PrivacyProperties();

    properties.setWithdrawnRetentionDays(7);

    assertThat(properties.getWithdrawnRetentionDays()).isEqualTo(7);
  }
}
