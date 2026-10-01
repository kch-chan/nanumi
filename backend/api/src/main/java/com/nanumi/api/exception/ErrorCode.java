package com.nanumi.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  // 요청 자체가 잘못된 경우임. 본문 JSON 이 깨졌거나 형식이 맞지 않음
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),

  // 필수 약관에 동의하지 않은 채로 가입을 시도한 경우임
  // 화면에서는 "다음" 버튼이 막혀 있지만 그 버튼은 막는 장치가 아님.
  // 개발자 도구로 지울 수 있고 API 를 직접 부르면 거치지도 않으므로 서버가 다시 확인함
  TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "필수 약관에 동의해야 가입할 수 있습니다."),

  DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
  DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

  // 어느 값이 겹쳤는지 알아내지 못했을 때 쓰는 기본값임
  DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 등록된 정보입니다."),

  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
  EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

  // 로그인 무차별 대입을 막을 때 씀. 남은 시간은 알려 주지 않음
  LOGIN_ATTEMPT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요."),
  SIGNUP_ATTEMPT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "가입 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요."),

  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
  WITHDRAWN_USER(HttpStatus.FORBIDDEN, "이미 탈퇴한 계정입니다."),

  // 아래 셋은 스프링이 먼저 걸러 내는 상황임. 응답 모양을 나머지와 맞추려고 코드로 들고 있음
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다."),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
  UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 형식입니다."),

  // 미처 잡지 못한 예외임. 원인은 로그에만 남기고 회원에게는 알리지 않음
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "요청을 처리하지 못했습니다.");

  private final HttpStatus status;
  private final String message;

  ErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }
}
