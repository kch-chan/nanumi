package com.nanumi.api.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 리프레시 토큰은 이제 이 엔티티가 아니라 RefreshToken 에 있음 (RefreshTokenTest 참고)
@DisplayName("계정 엔티티")
class AccountTest {

  private Account newAccount() {
    User user = User.builder().nickname("나눔이").aptName("행복아파트").build();
    return Account.builder()
        .user(user)
        .email("nanumi@example.com")
        .password("$nanumi$1$100000$s$h")
        .build();
  }

  @Test
  @DisplayName("만든 값이 그대로 담김")
  void 기본값() {
    Account account = newAccount();

    assertThat(account.getEmail()).isEqualTo("nanumi@example.com");
    assertThat(account.getUser().getNickname()).isEqualTo("나눔이");
  }

  @Test
  @DisplayName("비밀번호를 바꿀 수 있음")
  void 비밀번호_변경() {
    Account account = newAccount();

    account.changePassword("$nanumi$1$100000$new$hash");

    assertThat(account.getPassword()).isEqualTo("$nanumi$1$100000$new$hash");
  }
}
