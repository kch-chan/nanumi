package com.nanumi.api.security.password;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 비밀번호 해시 파라미터임. application.yml 의 nanumi.security.password 아래에서 읽어 옴
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "nanumi.security.password")
public class NanumiPasswordProperties {

  // PBKDF2 반복 횟수임. 값을 올려도 기존 해시는 자기 반복 횟수로 검증되므로 로그인은 계속 됨
  private int iterations = 210_000;

  // 서버만 아는 비밀값임. DB 가 통째로 유출돼도 이 값 없이는 대입 공격이 어려움
  // 운영에서는 반드시 환경 변수로 주입해야 함
  private String pepper = "";
}
