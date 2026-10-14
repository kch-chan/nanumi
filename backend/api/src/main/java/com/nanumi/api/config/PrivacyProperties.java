package com.nanumi.api.config;

import jakarta.annotation.PostConstruct;
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

  // 보유 기한이 잘못 들어온 채로 뜨는 것을 막음
  //
  // 왜 기동을 막는가: WithdrawnDataPurgeService 는 now.minusDays(이 값) 을 기준 시각으로 쓴다.
  // 0 이면 탈퇴한 사람이 그 즉시 파기 대상이 되고, 음수면 기준 시각이 미래가 되어
  // 탈퇴한 모든 회원이 한 번에 파기된다. 되돌릴 수 없는 삭제이므로 값을 받는 자리에서 막음
  @PostConstruct
  void valueCheck() {
    if (withdrawnRetentionDays < 1) {
      throw new IllegalStateException(
          "nanumi.privacy.withdrawn-retention-days 는 1 이상이어야 함: "
              + withdrawnRetentionDays
              + " (0 이하면 탈퇴한 회원이 즉시 파기됨. WITHDRAWN_RETENTION_DAYS 환경 변수를 확인해야 함)");
    }
  }
}
