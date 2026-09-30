package com.nanumi.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 개인정보를 얼마나 들고 있다가 지울지 정함
//
// 이 값은 화면에 보여 주는 약관(frontend/src/constants/terms.ts)의 "회원 탈퇴 후 30일까지" 와
// 같아야 함. 한쪽만 고치면 적어 둔 것과 실제 동작이 달라짐
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "nanumi.privacy")
public class PrivacyProperties {

  private int withdrawnRetentionDays = 30;
}
