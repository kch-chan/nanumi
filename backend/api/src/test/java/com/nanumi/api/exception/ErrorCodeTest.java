package com.nanumi.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

@DisplayName("오류 코드")
class ErrorCodeTest {

  @ParameterizedTest(name = "{0} 은 {1}")
  @CsvSource({
    "DUPLICATE_EMAIL, 409",
    "DUPLICATE_NICKNAME, 409",
    "INVALID_CREDENTIALS, 401",
    "INVALID_TOKEN, 401",
    "EXPIRED_REFRESH_TOKEN, 401",
    "ACCESS_DENIED, 403",
    "WITHDRAWN_USER, 403",
    "USER_NOT_FOUND, 404",
    "LOGIN_ATTEMPT_EXCEEDED, 429",
    "INTERNAL_ERROR, 500",
  })
  @DisplayName("정해 둔 상태 코드를 가짐")
  void 상태_코드(ErrorCode errorCode, int expected) {
    assertThat(errorCode.getStatus().value()).isEqualTo(expected);
  }

  @Test
  @DisplayName("모든 코드가 비어 있지 않은 메시지를 가짐")
  void 메시지가_있음() {
    assertThat(ErrorCode.values()).allSatisfy(code -> assertThat(code.getMessage()).isNotBlank());
  }

  // 로그인 실패 메시지가 "없는 계정" 과 "틀린 비밀번호" 로 갈리면
  // 그것만으로 가입 여부를 알아낼 수 있음. 하나만 있어야 함
  @Test
  @DisplayName("로그인 실패 메시지는 이메일과 비밀번호를 구분하지 않음")
  void 로그인_실패_메시지는_뭉뚱그림() {
    assertThat(ErrorCode.INVALID_CREDENTIALS.getMessage()).isEqualTo("이메일 또는 비밀번호가 올바르지 않습니다.");
  }

  // 회원에게 내부 사정을 알리면 공격의 단서가 됨
  @Test
  @DisplayName("500 메시지에는 원인이 드러나지 않음")
  void 내부_오류는_뭉뚱그림() {
    assertThat(ErrorCode.INTERNAL_ERROR.getMessage()).isEqualTo("요청을 처리하지 못했습니다.");
  }

  @Test
  @DisplayName("2xx 를 쓰는 오류 코드는 없음")
  void 성공_코드는_없음() {
    assertThat(Arrays.stream(ErrorCode.values()).map(ErrorCode::getStatus))
        .allSatisfy(status -> assertThat(status.isError()).isTrue());
  }

  @Test
  @DisplayName("잠금 안내에 남은 시간을 적지 않음")
  void 잠금_안내() {
    assertThat(ErrorCode.LOGIN_ATTEMPT_EXCEEDED.getStatus())
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    // 남은 시간을 알려 주면 잠금이 언제 풀리는지 맞춰서 다시 시도할 수 있게 됨
    assertThat(ErrorCode.LOGIN_ATTEMPT_EXCEEDED.getMessage()).doesNotContainPattern("[0-9]");
  }
}
