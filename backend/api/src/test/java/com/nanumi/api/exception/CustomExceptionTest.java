package com.nanumi.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("업무 예외")
class CustomExceptionTest {

  @Test
  @DisplayName("던질 때 넣은 오류 코드를 그대로 들고 있음")
  void 오류_코드_보관() {
    CustomException e = new CustomException(ErrorCode.DUPLICATE_EMAIL);

    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL);
  }

  // 예외 메시지를 ErrorCode 의 문구로 맞춰 두는 이유
  //
  // 로그에는 보통 예외 메시지만 찍힘. 여기가 비어 있으면 로그에 "CustomException" 만 남아
  // 어떤 상황이었는지 알 수 없음. 응답 본문을 만드는 쪽은 getErrorCode() 를 쓰므로
  // 이 값이 응답에 직접 나가지는 않지만, 두 곳이 어긋나면 로그와 응답이 서로 다른 말을 하게 됨
  @Test
  @DisplayName("예외 메시지는 오류 코드의 문구와 같음")
  void 메시지_일치() {
    CustomException e = new CustomException(ErrorCode.WITHDRAWN_USER);

    assertThat(e.getMessage()).isEqualTo(ErrorCode.WITHDRAWN_USER.getMessage());
    assertThat(e.getMessage()).isEqualTo("이미 탈퇴한 계정입니다.");
  }

  // RuntimeException(=unchecked)을 물려받아야 하는 이유
  //
  // 1) 스프링의 기본 롤백 규칙은 unchecked 예외에만 걸림. checked 로 만들면 AuthService 에서
  //    예외를 던져도 앞서 저장한 행이 그대로 커밋됨
  // 2) 서비스 메서드마다 throws 를 달 필요가 없어 Controller 가 업무 예외를 알지 않아도 됨
  @Test
  @DisplayName("RuntimeException 이라 트랜잭션 롤백 대상임")
  void 롤백_대상() {
    assertThat(new CustomException(ErrorCode.INVALID_TOKEN)).isInstanceOf(RuntimeException.class);
  }

  // 재사용 감지(AuthService.refresh)는 이 성질을 거꾸로 이용함
  // 토큰 행을 지운 뒤 예외를 던져야 하므로 그 메서드에만 noRollbackFor 를 붙여 둠
  @Test
  @DisplayName("모든 오류 코드로 만들 수 있음")
  void 모든_코드로_생성() {
    for (ErrorCode code : ErrorCode.values()) {
      CustomException e = new CustomException(code);

      assertThat(e.getErrorCode()).isEqualTo(code);
      assertThat(e.getMessage()).isNotBlank();
    }
  }
}
