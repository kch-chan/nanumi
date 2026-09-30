package com.nanumi.api.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("리프레시 토큰 엔티티")
class RefreshTokenTest {

  private static final String HASH = "a".repeat(64);

  private Account newAccount() {
    User user = User.builder().nickname("나눔이").aptName("행복아파트").build();
    return Account.builder().user(user).email("nanumi@example.com").password("h").build();
  }

  private RefreshToken tokenExpiringAt(LocalDateTime expiresAt) {
    return RefreshToken.builder()
        .account(newAccount())
        .tokenHash(HASH)
        .expiresAt(expiresAt)
        .build();
  }

  @Test
  @DisplayName("만든 값이 그대로 담김")
  void 기본값() {
    RefreshToken token = tokenExpiringAt(LocalDateTime.now().plusDays(14));

    assertThat(token.getTokenHash()).isEqualTo(HASH);
    assertThat(token.getAccount().getEmail()).isEqualTo("nanumi@example.com");
  }

  @Test
  @DisplayName("기한이 지났으면 만료임")
  void 지난_기한() {
    assertThat(tokenExpiringAt(LocalDateTime.now().minusSeconds(1)).isExpired()).isTrue();
  }

  @Test
  @DisplayName("기한이 남았으면 만료가 아님")
  void 남은_기한() {
    assertThat(tokenExpiringAt(LocalDateTime.now().plusDays(14)).isExpired()).isFalse();
  }

  // SHA-256 16진수는 항상 64자임. 컬럼 길이가 이 값과 어긋나면 저장할 때 잘림
  @Test
  @DisplayName("해시 칸 길이는 64 임")
  void 해시_길이_상수() {
    assertThat(RefreshToken.TOKEN_HASH_LENGTH).isEqualTo(64);
  }
}
