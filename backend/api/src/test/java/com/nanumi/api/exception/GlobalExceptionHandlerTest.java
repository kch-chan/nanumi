package com.nanumi.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanumi.api.dto.response.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.ServletWebRequest;

@DisplayName("전역 예외 처리")
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("CustomException 은 코드에 적힌 상태와 메시지로 나감")
  void 커스텀_예외() {
    ResponseEntity<ErrorResponse> response =
        handler.handleCustomException(new CustomException(ErrorCode.DUPLICATE_EMAIL));

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody().message()).isEqualTo("이미 사용 중인 이메일입니다.");
    assertThat(response.getBody().status()).isEqualTo(409);
  }

  // 중복 검사와 저장 사이에 다른 요청이 끼어들면 DB 제약에서 걸림
  // 이걸 놓치면 "이미 사용 중인 이메일" 이어야 할 응답이 500 으로 나감
  @Test
  @DisplayName("이메일 제약에 걸리면 409 이메일 중복으로 바꿔 줌")
  void 이메일_제약() {
    ResponseEntity<ErrorResponse> response =
        handler.handleDataIntegrityViolation(
            new DataIntegrityViolationException(
                "포장 메시지", new RuntimeException("Duplicate entry for key 'uk_accounts_email'")));

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody().message()).isEqualTo("이미 사용 중인 이메일입니다.");
  }

  @Test
  @DisplayName("닉네임 제약에 걸리면 409 닉네임 중복으로 바꿔 줌")
  void 닉네임_제약() {
    ResponseEntity<ErrorResponse> response =
        handler.handleDataIntegrityViolation(
            new DataIntegrityViolationException(
                "포장 메시지", new RuntimeException("Duplicate entry for key 'uk_users_nickname'")));

    assertThat(response.getBody().message()).isEqualTo("이미 사용 중인 닉네임입니다.");
  }

  @Test
  @DisplayName("어느 값이 겹쳤는지 모르면 일반 문구로 나감")
  void 알_수_없는_제약() {
    ResponseEntity<ErrorResponse> response =
        handler.handleDataIntegrityViolation(
            new DataIntegrityViolationException("포장", new RuntimeException("뭔가 다른 제약")));

    assertThat(response.getBody().message()).isEqualTo("이미 등록된 정보입니다.");
  }

  // 원인은 로그에만 남기고 회원에게는 정해 둔 문구만 돌려줘야 함
  @Test
  @DisplayName("예상 못 한 예외는 500 고정 문구로 나가고 원인이 새지 않음")
  void 예상하지_못한_예외() {
    ResponseEntity<ErrorResponse> response =
        handler.handleUnexpectedException(new IllegalStateException("DB 비밀번호가 틀렸습니다"));

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody().message()).isEqualTo("요청을 처리하지 못했습니다.");
    assertThat(response.getBody().message()).doesNotContain("DB");
  }

  // 스프링이 만든 ProblemDetail 이 그대로 나가면 프런트가 message 를 못 읽음
  @Test
  @DisplayName("스프링이 만든 본문은 우리 형식으로 바꿔서 내보냄")
  void 스프링_본문_교체() {
    Object body =
        invokeHandleExceptionInternal(
            new RuntimeException("깨진 JSON"), null, HttpStatus.BAD_REQUEST);

    assertThat(body).isInstanceOf(ErrorResponse.class);
    ErrorResponse error = (ErrorResponse) body;
    assertThat(error.status()).isEqualTo(400);
    assertThat(error.message()).isEqualTo("요청 형식이 올바르지 않습니다.");
  }

  @Test
  @DisplayName("우리가 넣은 본문은 그대로 둠")
  void 우리_본문은_유지() {
    ErrorResponse ours = ErrorResponse.of(400, "닉네임을 입력해 주세요.");

    Object body =
        invokeHandleExceptionInternal(new RuntimeException("검증 실패"), ours, HttpStatus.BAD_REQUEST);

    assertThat(body).isSameAs(ours);
  }

  @Test
  @DisplayName("405 와 415 도 각각의 문구로 바뀜")
  void 상태별_문구() {
    assertThat(
            ((ErrorResponse)
                    invokeHandleExceptionInternal(
                        new RuntimeException("x"), null, HttpStatus.METHOD_NOT_ALLOWED))
                .message())
        .isEqualTo("지원하지 않는 요청 방식입니다.");

    assertThat(
            ((ErrorResponse)
                    invokeHandleExceptionInternal(
                        new RuntimeException("x"), null, HttpStatus.UNSUPPORTED_MEDIA_TYPE))
                .message())
        .isEqualTo("지원하지 않는 형식입니다.");

    assertThat(
            ((ErrorResponse)
                    invokeHandleExceptionInternal(
                        new RuntimeException("x"), null, HttpStatus.NOT_FOUND))
                .message())
        .isEqualTo("요청한 경로를 찾을 수 없습니다.");
  }

  // handleExceptionInternal 은 protected 라서 직접 부를 수 없음
  // 상위 클래스가 만들어 주는 응답에서 본문만 꺼내 봄
  private Object invokeHandleExceptionInternal(Exception e, Object body, HttpStatus status) {
    ResponseEntity<Object> response =
        (ResponseEntity<Object>)
            ReflectionTestUtils.invokeMethod(
                handler,
                "handleExceptionInternal",
                e,
                body,
                new HttpHeaders(),
                status,
                new ServletWebRequest(new MockHttpServletRequest()));
    return response.getBody();
  }
}
