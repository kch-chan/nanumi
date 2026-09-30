package com.nanumi.api.exception;

import com.nanumi.api.dto.response.ErrorResponse;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// ResponseEntityExceptionHandler 를 물려받는 이유는,
// 안 그러면 스프링이 던지는 표준 예외(405, 404, 415 등)가 아래 Exception 핸들러에 걸려
// 전부 500 으로 나가기 때문임. 예를 들어 GET /api/auth/login 한 번이면 바로 재현됨
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
    ErrorCode errorCode = e.getErrorCode();
    return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
  }

  // 중복 검사와 저장 사이에 다른 요청이 끼어들면 검사를 둘 다 통과한 뒤 DB 제약에서 걸림
  // 이걸 놓치면 "이미 사용 중인 이메일" 이어야 할 응답이 500 으로 나감
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException e) {
    ErrorCode errorCode = resolveConstraint(e);
    log.debug("무결성 제약에 걸림: {}", errorCode, e);
    return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
  }

  // 위에서 잡지 못한 예외임
  // 원인은 로그에만 남기고, 회원에게는 정해 둔 문구만 돌려줌
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
    log.error("처리하지 못한 예외가 발생함", e);
    return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
        .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
  }

  // 어느 칸이 왜 틀렸는지는 알려 줘야 해서 기본 처리 대신 필드 메시지를 모아서 내보냄
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));

    Object body = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message);
    return handleExceptionInternal(e, body, headers, HttpStatus.BAD_REQUEST, request);
  }

  // 스프링이 만들어 주는 ProblemDetail 대신 우리 ErrorResponse 모양으로 바꿔서 내보냄
  // 프런트가 응답에서 message 하나만 꺼내 쓰므로 형식이 갈리면 안 됨
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception e,
      Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    if (statusCode.is5xxServerError()) {
      log.error("스프링이 처리한 5xx 예외임", e);
    } else {
      log.debug("스프링이 처리한 4xx 예외임: {}", e.getMessage());
    }

    // 스프링이 만든 ProblemDetail 이 body 로 넘어오는 경로가 있으므로(깨진 JSON 등),
    // 우리가 직접 넣은 ErrorResponse 가 아니면 무조건 우리 형식으로 바꿔서 내보냄
    Object errorBody =
        (body instanceof ErrorResponse)
            ? body
            : ErrorResponse.of(statusCode.value(), messageOf(statusCode));
    return super.handleExceptionInternal(e, errorBody, headers, statusCode, request);
  }

  private String messageOf(HttpStatusCode statusCode) {
    if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
      return ErrorCode.RESOURCE_NOT_FOUND.getMessage();
    }
    if (statusCode.value() == HttpStatus.METHOD_NOT_ALLOWED.value()) {
      return ErrorCode.METHOD_NOT_ALLOWED.getMessage();
    }
    if (statusCode.value() == HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()) {
      return ErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage();
    }
    return statusCode.is5xxServerError()
        ? ErrorCode.INTERNAL_ERROR.getMessage()
        : ErrorCode.INVALID_REQUEST.getMessage();
  }

  // 제약 이름으로 어느 값이 겹쳤는지 가려냄
  // 이름을 읽어야 해서 엔티티에서 유니크 제약에 직접 이름을 붙여 두었음
  private ErrorCode resolveConstraint(DataIntegrityViolationException e) {
    Throwable cause = e.getMostSpecificCause();
    String detail = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase(Locale.ROOT);

    if (detail.contains("uk_accounts_email")) {
      return ErrorCode.DUPLICATE_EMAIL;
    }
    if (detail.contains("uk_users_nickname")) {
      return ErrorCode.DUPLICATE_NICKNAME;
    }
    return ErrorCode.DUPLICATE_RESOURCE;
  }
}
