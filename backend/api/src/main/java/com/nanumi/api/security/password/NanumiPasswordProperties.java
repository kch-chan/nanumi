package com.nanumi.api.security.password;

import jakarta.annotation.PostConstruct;
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
  private String pepper = "";

  // pepper 가 잘못 들어온 채로 뜨는 것을 막음
  @PostConstruct
  void valueCheck() {
    if (pepper == null || pepper.isBlank()) {
      throw new IllegalStateException(
          "nanumi.security.password.pepper 가 비어 있음. PASSWORD_PEPPER 환경 변수를 넣어야 함");
    }
    if (pepper.contains("${")) {
      throw new IllegalStateException(
          "nanumi.security.password.pepper 에 치환되지 않은 자리표시자가 들어옴: "
              + pepper
              + " (PASSWORD_PEPPER 환경 변수가 전달되지 않음)");
    }
  }
}
