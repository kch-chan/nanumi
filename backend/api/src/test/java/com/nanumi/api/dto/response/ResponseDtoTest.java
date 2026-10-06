package com.nanumi.api.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanumi.api.entity.User;
import com.nanumi.api.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// 응답 DTO 의 문구는 화면에 그대로 보임.
// 한 글자만 바뀌어도 사용자가 보는 안내가 달라지므로 고정 문구를 여기서 못 박아 둠
//
// UserResponse 는 "무엇을 내보내고 무엇을 내보내지 않는가" 가 핵심임
@DisplayName("응답 DTO")
class ResponseDtoTest {

  private User newUser() {
    return User.builder().nickname("나눔이").aptName("행복아파트").dong("101").ho("1502").build();
  }

  @Nested
  @DisplayName("회원 정보")
  class UserResponseTest {

    @Test
    @DisplayName("엔티티의 값이 그대로 옮겨짐")
    void 변환() {
      UserResponse response = UserResponse.from(newUser());

      assertThat(response.nickname()).isEqualTo("나눔이");
      assertThat(response.aptName()).isEqualTo("행복아파트");
      assertThat(response.dong()).isEqualTo("101");
      assertThat(response.ho()).isEqualTo("1502");
    }

    // 가입할 때 role 을 받지 않음. 요청으로 넘어온 값이 아니라 엔티티 기본값이 나가야 함
    @Test
    @DisplayName("권한은 문자열로 나감")
    void 권한_문자열() {
      assertThat(UserResponse.from(newUser()).role()).isEqualTo("USER");
    }

    @Test
    @DisplayName("동·호가 없으면 null 로 나감")
    void 선택_항목_없음() {
      User user = User.builder().nickname("나눔이").aptName("행복아파트").build();

      UserResponse response = UserResponse.from(user);

      assertThat(response.dong()).isNull();
      assertThat(response.ho()).isNull();
    }

    // 이메일과 비밀번호 해시는 이 record 에 칸 자체가 없음.
    // 칸을 더하면 이 테스트가 깨지므로, 내보낼 값을 늘릴 때 한 번 더 생각하게 됨
    @Test
    @DisplayName("이메일과 비밀번호는 내보내지 않음")
    void 민감_정보_제외() {
      String[] components =
          java.util.Arrays.stream(UserResponse.class.getRecordComponents())
              .map(java.lang.reflect.RecordComponent::getName)
              .toArray(String[]::new);

      assertThat(components)
          .containsExactlyInAnyOrder("id", "nickname", "aptName", "dong", "ho", "role");
    }
  }

  @Nested
  @DisplayName("고정 문구")
  class MessageTest {

    @Test
    @DisplayName("회원가입 완료 문구")
    void 회원가입() {
      SignupResponse response = SignupResponse.of(UserResponse.from(newUser()));

      assertThat(response.message()).isEqualTo("회원가입에 성공하였습니다");
      assertThat(response.user().nickname()).isEqualTo("나눔이");
    }

    @Test
    @DisplayName("로그아웃 완료 문구")
    void 로그아웃() {
      assertThat(LogoutResponse.of().message()).isEqualTo("로그아웃이 완료되었습니다.");
    }

    @Test
    @DisplayName("회원탈퇴 완료 문구와 탈퇴 시각")
    void 회원탈퇴() {
      LocalDateTime withdrawnAt = LocalDateTime.of(2026, 10, 1, 12, 0);

      WithdrawalResponse response = WithdrawalResponse.of(withdrawnAt);

      assertThat(response.message()).isEqualTo("회원탈퇴가 완료되었습니다.");
      assertThat(response.withdrawnAt()).isEqualTo(withdrawnAt);
    }
  }

  @Nested
  @DisplayName("토큰 응답")
  class TokenTest {

    @Test
    @DisplayName("로그인은 토큰 2종과 회원 정보를 함께 내보냄")
    void 로그인() {
      LoginResponse response = LoginResponse.of("access", "refresh", UserResponse.from(newUser()));

      assertThat(response.accessToken()).isEqualTo("access");
      assertThat(response.refreshToken()).isEqualTo("refresh");
      assertThat(response.user()).isNotNull();
    }

    // 재발급은 회원 정보를 담지 않음. 토큰만 갈아 끼우는 요청이기 때문
    @Test
    @DisplayName("재발급은 토큰만 내보냄")
    void 재발급() {
      TokenResponse response = TokenResponse.of("access", "refresh");

      assertThat(response.accessToken()).isEqualTo("access");
      assertThat(response.refreshToken()).isEqualTo("refresh");
      assertThat(TokenResponse.class.getRecordComponents()).hasSize(2);
    }
  }

  @Nested
  @DisplayName("오류 응답")
  class ErrorResponseTest {

    // 모든 오류가 {status, message} 한 가지 모양으로 나가야 프런트가 message 하나만 꺼내 쓸 수 있음
    @Test
    @DisplayName("오류 코드에서 상태와 문구를 가져옴")
    void 코드로_생성() {
      ErrorResponse response = ErrorResponse.of(ErrorCode.DUPLICATE_EMAIL);

      assertThat(response.status()).isEqualTo(409);
      assertThat(response.message()).isEqualTo("이미 사용 중인 이메일입니다.");
    }

    // 검증 실패처럼 ErrorCode 로 묶을 수 없는 경우를 위한 입구임
    @Test
    @DisplayName("상태와 문구를 직접 넣을 수도 있음")
    void 직접_생성() {
      ErrorResponse response = ErrorResponse.of(400, "비밀번호는 8~20자여야 합니다.");

      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message()).isEqualTo("비밀번호는 8~20자여야 합니다.");
    }

    @Test
    @DisplayName("본문에는 상태와 문구 두 칸만 있음")
    void 칸_구성() {
      assertThat(ErrorResponse.class.getRecordComponents()).hasSize(2);
    }
  }
}
