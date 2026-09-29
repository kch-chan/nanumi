package com.nanumi.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JWT 설정값")
class JwtConfigTest {

  // 키를 넣는 방법이 경로와 PEM 본문 두 가지임
  // 본문 자리를 비워 두면 JwtTokenProvider 가 경로 쪽을 읽음
  @Test
  @DisplayName("아무것도 넣지 않으면 전부 비어 있음")
  void 기본값() {
    JwtConfig config = new JwtConfig();

    assertThat(config.getPrivateKeyPath()).isNull();
    assertThat(config.getPublicKeyPath()).isNull();
    assertThat(config.getPrivateKey()).isNull();
    assertThat(config.getPublicKey()).isNull();
    assertThat(config.getAccessTokenExpiration()).isZero();
    assertThat(config.getRefreshTokenExpiration()).isZero();
  }

  @Test
  @DisplayName("키 파일 경로를 담을 수 있음")
  void 경로_설정() {
    JwtConfig config = new JwtConfig();

    config.setPrivateKeyPath("file:./keys/private_key.pem");
    config.setPublicKeyPath("classpath:keys/public_key.pem");

    assertThat(config.getPrivateKeyPath()).isEqualTo("file:./keys/private_key.pem");
    assertThat(config.getPublicKeyPath()).isEqualTo("classpath:keys/public_key.pem");
  }

  // CI 나 컨테이너처럼 비밀 저장소가 파일 대신 문자열만 줄 수 있는 환경을 위한 자리임
  @Test
  @DisplayName("PEM 본문을 직접 담을 수 있음")
  void 본문_설정() {
    JwtConfig config = new JwtConfig();

    config.setPrivateKey("-----BEGIN PRIVATE KEY-----\nAAA\n-----END PRIVATE KEY-----");

    assertThat(config.getPrivateKey()).contains("BEGIN PRIVATE KEY");
  }

  // 액세스 토큰은 서버에서 따로 폐기할 수 없으므로 수명을 짧게 잡아야 함
  @Test
  @DisplayName("유효 기간을 밀리초로 담음")
  void 유효_기간() {
    JwtConfig config = new JwtConfig();

    config.setAccessTokenExpiration(900_000L);
    config.setRefreshTokenExpiration(1_209_600_000L);

    assertThat(config.getAccessTokenExpiration()).isEqualTo(900_000L);
    assertThat(config.getRefreshTokenExpiration()).isEqualTo(1_209_600_000L);
    assertThat(config.getAccessTokenExpiration()).isLessThan(config.getRefreshTokenExpiration());
  }
}
