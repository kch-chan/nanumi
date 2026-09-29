package com.nanumi.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanumi.api.config.JwtConfig;
import com.nanumi.api.security.JwtTokenProvider.TokenType;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 키 파일을 두지 않고, 테스트 안에서 만든 키를 PEM 본문으로 넘겨서 확인함
// 운영에서 비밀 저장소가 파일 대신 문자열로 키를 줄 때와 같은 경로임
@DisplayName("JWT 토큰")
class JwtTokenProviderTest {

  private static String privateKeyPem;
  private static String publicKeyPem;

  @BeforeAll
  static void generateKeys() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair pair = generator.generateKeyPair();

    privateKeyPem = pem("PRIVATE KEY", pair.getPrivate().getEncoded());
    publicKeyPem = pem("PUBLIC KEY", pair.getPublic().getEncoded());
  }

  private static String pem(String type, byte[] der) {
    return "-----BEGIN "
        + type
        + "-----\n"
        + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(der)
        + "\n-----END "
        + type
        + "-----\n";
  }

  private JwtTokenProvider providerWith(long accessExpiration) throws Exception {
    JwtConfig config = new JwtConfig();
    config.setPrivateKey(privateKeyPem);
    config.setPublicKey(publicKeyPem);
    config.setAccessTokenExpiration(accessExpiration);
    config.setRefreshTokenExpiration(1_209_600_000L);

    JwtTokenProvider provider = new JwtTokenProvider(config);
    // 스프링이 불러 주는 초기화 메서드를 직접 부름
    Method init = JwtTokenProvider.class.getDeclaredMethod("init");
    init.setAccessible(true);
    init.invoke(provider);
    return provider;
  }

  @Test
  @DisplayName("액세스 토큰을 만들고 다시 읽으면 회원 번호가 나옴")
  void 액세스_토큰_왕복() throws Exception {
    JwtTokenProvider provider = providerWith(900_000L);

    String token = provider.createAccessToken(42L);

    assertThat(provider.resolveUserId(token, TokenType.ACCESS)).contains(42L);
  }

  // 종류를 구분하지 않으면 14일짜리 리프레시 토큰을 액세스 토큰처럼 쓸 수 있게 됨
  @Test
  @DisplayName("리프레시 토큰은 액세스 토큰 자리에서 거부됨")
  void 리프레시_토큰은_액세스로_못_씀() throws Exception {
    JwtTokenProvider provider = providerWith(900_000L);

    String refresh = provider.createRefreshToken(42L);

    assertThat(provider.resolveUserId(refresh, TokenType.REFRESH)).contains(42L);
    assertThat(provider.resolveUserId(refresh, TokenType.ACCESS)).isEmpty();
  }

  @Test
  @DisplayName("만료된 토큰은 거부됨")
  void 만료된_토큰은_거부됨() throws Exception {
    // 유효 기간을 음수로 줘서 이미 만료된 토큰을 만듦
    JwtTokenProvider provider = providerWith(-1_000L);

    String token = provider.createAccessToken(42L);

    assertThat(provider.resolveUserId(token, TokenType.ACCESS)).isEmpty();
  }

  @Test
  @DisplayName("서명이 망가진 토큰은 거부됨")
  void 망가진_토큰은_거부됨() throws Exception {
    JwtTokenProvider provider = providerWith(900_000L);

    String token = provider.createAccessToken(42L);

    assertThat(provider.resolveUserId(token + "x", TokenType.ACCESS)).isEmpty();
    assertThat(provider.resolveUserId("전혀 토큰이 아님", TokenType.ACCESS)).isEmpty();
    assertThat(provider.resolveUserId(null, TokenType.ACCESS)).isEmpty();
  }
}
