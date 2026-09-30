package com.nanumi.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanumi.api.entity.Account;
import com.nanumi.api.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

// JPA 관련 빈만 올림. 웹·시큐리티는 뜨지 않음
//
// 메서드 이름으로 만들어지는 쿼리(findByUser_Id 등)는 이름이 한 글자만 틀려도
// 앱을 띄울 때 터진다. 그걸 여기서 미리 잡음
// DB 는 실제 PostgreSQL 임(H2 아님). 그래서 db/migration 의 SQL 이 실제로 도는지,
// 엔티티와 스키마가 맞는지까지 여기서 확인됨 (src/test/resources/application-test.yml)
@DataJpaTest
// 임베디드 DB 로 갈아치우지 않도록 막음. application-test.yml 의 PostgreSQL 을 그대로 씀
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("저장소")
class RepositoryTest {

  @Autowired private UserRepository userRepository;
  @Autowired private AccountRepository accountRepository;

  private User user;

  @BeforeEach
  void setUp() {
    user =
        userRepository.save(
            User.builder().nickname("나눔이").aptName("행복아파트").dong("101").ho("1502").build());
    accountRepository.save(
        Account.builder()
            .user(user)
            .email("nanumi@example.com")
            .password("$nanumi$1$210000$s$h")
            .build());
  }

  @Test
  @DisplayName("저장하면 번호와 생성 시각이 채워짐")
  void 저장() {
    // id 가 int 라 null 검사는 뜻이 없음. 실제로 번호가 부여됐는지를 봄
    assertThat(user.getId()).isPositive();
    assertThat(user.getCreatedAt()).isNotNull();
    assertThat(user.getUpdatedAt()).isNotNull();
  }

  @Test
  @DisplayName("이메일로 계정을 찾음")
  void 이메일로_찾기() {
    assertThat(accountRepository.findByEmail("nanumi@example.com")).isPresent();
    assertThat(accountRepository.findByEmail("없는사람@example.com")).isEmpty();
  }

  @Test
  @DisplayName("회원 번호로 계정을 찾음")
  void 회원_번호로_찾기() {
    assertThat(accountRepository.findByUser_Id(user.getId())).isPresent();
    assertThat(accountRepository.findByUser_Id(999)).isEmpty();
  }

  @Test
  @DisplayName("이메일 존재 여부를 확인함")
  void 이메일_존재_확인() {
    assertThat(accountRepository.existsByEmail("nanumi@example.com")).isTrue();
    assertThat(accountRepository.existsByEmail("없는사람@example.com")).isFalse();
  }

  // abc 와 ABC 가 따로 존재하면 사람이 헷갈림
  @Test
  @DisplayName("닉네임 중복 검사는 대소문자를 무시함")
  void 닉네임_대소문자_무시() {
    userRepository.save(User.builder().nickname("Nanumi").aptName("행복아파트").build());

    assertThat(userRepository.existsByNicknameIgnoreCase("nanumi")).isTrue();
    assertThat(userRepository.existsByNicknameIgnoreCase("NANUMI")).isTrue();
    assertThat(userRepository.existsByNicknameIgnoreCase("다른이름")).isFalse();
  }

  @Test
  @DisplayName("닉네임으로 회원을 찾음")
  void 닉네임으로_찾기() {
    assertThat(userRepository.findByNickname("나눔이")).isPresent();
    assertThat(userRepository.findByNickname("없는이름")).isEmpty();
  }

  // 나눔글은 같은 단지 안에서만 보이므로 단지 이름으로 모으는 조회가 필요함
  @Test
  @DisplayName("같은 단지 회원을 모아서 찾음")
  void 단지로_찾기() {
    userRepository.save(User.builder().nickname("이웃").aptName("행복아파트").build());
    userRepository.save(User.builder().nickname("남남").aptName("다른아파트").build());

    assertThat(userRepository.findByAptName("행복아파트")).hasSize(2);
    assertThat(userRepository.findByAptName("다른아파트")).hasSize(1);
    assertThat(userRepository.findByAptName("없는아파트")).isEmpty();
  }

  @Test
  @DisplayName("계정에서 회원을 따라갈 수 있음")
  void 연관관계() {
    Account account = accountRepository.findByEmail("nanumi@example.com").orElseThrow();

    assertThat(account.getUser().getNickname()).isEqualTo("나눔이");
  }
}
