package com.nanumi.api.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("계정 엔티티")
class AccountTest {

  private static final String HASH = "a".repeat(64);

  private Account newAccount() {
    User user = User.builder().nickname("나눔이").aptName("행복아파트").build();
    return Account.builder()
        .user(user)
        .email("nanumi@example.com")
        .password("$nanumi$1$210000$s$h")
        .build();
  }

  @Test
  @DisplayName("새로 만들면 담아 둔 리프레시 토큰이 없음")
  void 기본값() {
    Account account = newAccount();

    assertThat(account.hasRefreshToken()).isFalse();
    assertThat(account.getRefreshTokenHash()).isNull();
  }

  // 담아 둔 게 아예 없으면 기한도 없으므로 만료로 봐야 함
  @Test
  @DisplayName("기한이 없으면 만료로 봄")
  void 기한_없으면_만료() {
    assertThat(newAccount().isExpired()).isTrue();
  }

  @Test
  @DisplayName("기한이 지났으면 만료임")
  void 지난_기한() {
    Account account = newAccount();

    account.updateRefreshToken(HASH, LocalDateTime.now().minusSeconds(1));

    assertThat(account.isExpired()).isTrue();
  }

  @Test
  @DisplayName("기한이 남았으면 만료가 아님")
  void 남은_기한() {
    Account account = newAccount();

    account.updateRefreshToken(HASH, LocalDateTime.now().plusDays(14));

    assertThat(account.isExpired()).isFalse();
    assertThat(account.hasRefreshToken()).isTrue();
    assertThat(account.getRefreshTokenHash()).isEqualTo(HASH);
  }

  @Test
  @DisplayName("지우면 해시와 기한이 둘 다 비워짐")
  void 토큰_지우기() {
    Account account = newAccount();
    account.updateRefreshToken(HASH, LocalDateTime.now().plusDays(14));

    account.clearRefreshToken();

    assertThat(account.hasRefreshToken()).isFalse();
    assertThat(account.getExpiryDate()).isNull();
    assertThat(account.isExpired()).isTrue();
  }

  @Test
  @DisplayName("비밀번호를 바꿀 수 있음")
  void 비밀번호_변경() {
    Account account = newAccount();

    account.changePassword("$nanumi$1$210000$new$hash");

    assertThat(account.getPassword()).isEqualTo("$nanumi$1$210000$new$hash");
  }

  // SHA-256 16진수는 항상 64자임. 컬럼 길이가 이 값과 어긋나면 저장할 때 잘림
  @Test
  @DisplayName("리프레시 토큰 해시 칸 길이는 64 임")
  void 해시_길이_상수() {
    assertThat(Account.REFRESH_TOKEN_HASH_LENGTH).isEqualTo(64);
  }
}
