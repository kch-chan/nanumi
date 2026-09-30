package com.nanumi.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nanumi.api.dto.request.LoginRequest;
import com.nanumi.api.dto.response.LoginResponse;
import com.nanumi.api.dto.response.LogoutResponse;
import com.nanumi.api.dto.response.SignupResponse;
import com.nanumi.api.dto.response.TokenResponse;
import com.nanumi.api.dto.response.UserResponse;
import com.nanumi.api.dto.response.WithdrawalResponse;
import com.nanumi.api.exception.CustomException;
import com.nanumi.api.exception.ErrorCode;
import com.nanumi.api.exception.GlobalExceptionHandler;
import com.nanumi.api.service.AuthService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// 스프링 전체 대신 이 컨트롤러 하나만 올려서 확인함
// 서비스는 가짜라 DB 가 필요 없고, 예외 처리기를 같이 붙여서 오류 응답 모양까지 봄
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("인증 컨트롤러")
class AuthControllerTest {

  @Mock private AuthService authService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new AuthController(authService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  private static final String SIGNUP_BODY =
      """
      {
        "email": "nanumi@example.com",
        "password": "Ab3!efgh",
        "nickname": "나눔이",
        "aptName": "행복아파트",
        "dong": "101",
        "ho": "1502"
      }
      """;

  private UserResponse user() {
    return new UserResponse(1, "나눔이", "행복아파트", "101", "1502", "USER");
  }

  @Test
  @DisplayName("회원가입에 성공하면 201 을 돌려줌")
  void 회원가입_성공() throws Exception {
    when(authService.signup(any(), anyString())).thenReturn(SignupResponse.of(user()));

    mockMvc
        .perform(
            post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(SIGNUP_BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.user.nickname").value("나눔이"));
  }

  // 컨트롤러에서 막아야 서비스까지 내려가지 않음
  @Test
  @DisplayName("입력이 잘못되면 400 과 이유를 돌려주고 서비스는 부르지 않음")
  void 회원가입_검증_실패() throws Exception {
    String body = SIGNUP_BODY.replace("Ab3!efgh", "short");

    mockMvc
        .perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").isNotEmpty());

    verify(authService, org.mockito.Mockito.never()).signup(any(), anyString());
  }

  @Test
  @DisplayName("이메일이 겹치면 409 로 나감")
  void 회원가입_중복() throws Exception {
    when(authService.signup(any(), anyString()))
        .thenThrow(new CustomException(ErrorCode.DUPLICATE_EMAIL));

    mockMvc
        .perform(
            post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(SIGNUP_BODY))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
  }

  @Test
  @DisplayName("로그인에 성공하면 토큰 두 개를 돌려줌")
  void 로그인_성공() throws Exception {
    when(authService.login(any(), anyString()))
        .thenReturn(LoginResponse.of("access-token", "refresh-token", user()));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nanumi@example.com\",\"password\":\"Ab3!efgh\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("access-token"))
        .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
  }

  // 실패 횟수를 이메일과 접속 IP 로 따로 세기 때문에 IP 를 같이 넘겨야 함
  @Test
  @DisplayName("로그인할 때 접속 IP 를 함께 넘김")
  void 로그인_IP_전달() throws Exception {
    when(authService.login(any(), anyString())).thenReturn(LoginResponse.of("a", "r", user()));

    mockMvc.perform(
        post("/api/auth/login")
            .with(
                request -> {
                  request.setRemoteAddr("203.0.113.7");
                  return request;
                })
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"nanumi@example.com\",\"password\":\"Ab3!efgh\"}"));

    ArgumentCaptor<String> ip = ArgumentCaptor.forClass(String.class);
    verify(authService).login(any(LoginRequest.class), ip.capture());
    org.assertj.core.api.Assertions.assertThat(ip.getValue()).isEqualTo("203.0.113.7");
  }

  @Test
  @DisplayName("비밀번호가 틀리면 401 로 나감")
  void 로그인_실패() throws Exception {
    when(authService.login(any(), anyString()))
        .thenThrow(new CustomException(ErrorCode.INVALID_CREDENTIALS));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nanumi@example.com\",\"password\":\"Ab3!efgh\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
  }

  @Test
  @DisplayName("잠긴 계정은 429 로 나감")
  void 로그인_잠김() throws Exception {
    when(authService.login(any(), anyString()))
        .thenThrow(new CustomException(ErrorCode.LOGIN_ATTEMPT_EXCEEDED));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nanumi@example.com\",\"password\":\"Ab3!efgh\"}"))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  @DisplayName("리프레시 토큰으로 새 토큰을 받음")
  void 토큰_재발급() throws Exception {
    when(authService.refresh(any())).thenReturn(TokenResponse.of("새-액세스", "새-리프레시"));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"옛-토큰\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("새-액세스"));
  }

  @Test
  @DisplayName("로그아웃은 200 으로 나감")
  void 로그아웃() throws Exception {
    when(authService.logout(anyInt(), any())).thenReturn(LogoutResponse.of());

    mockMvc
        .perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("탈퇴는 200 과 탈퇴 시각을 돌려줌")
  void 탈퇴() throws Exception {
    when(authService.withdraw(any(), any()))
        .thenReturn(WithdrawalResponse.of(LocalDateTime.of(2026, 1, 1, 0, 0)));

    mockMvc
        .perform(
            post("/api/auth/withdrawal")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"Ab3!efgh\",\"reason\":\"이사 갑니다\"}"))
        .andExpect(status().isOk());
  }

  // GET /api/auth/login 한 번이면 재현됨. 이걸 놓치면 500 으로 나감
  @Test
  @DisplayName("지원하지 않는 방식으로 부르면 405 로 나감")
  void 잘못된_메서드() throws Exception {
    mockMvc
        .perform(get("/api/auth/login"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.message").value("지원하지 않는 요청 방식입니다."));
  }

  // 스프링 기본 ProblemDetail 이 그대로 나가면 프런트가 message 를 못 읽음
  @Test
  @DisplayName("본문이 깨져 있어도 우리 형식으로 나감")
  void 깨진_JSON() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\": "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("요청 형식이 올바르지 않습니다."))
        .andExpect(jsonPath("$.detail").doesNotExist());
  }

  @Test
  @DisplayName("형식이 아닌 본문을 보내면 415 로 나감")
  void 잘못된_형식() throws Exception {
    mockMvc
        .perform(post("/api/auth/login").contentType(MediaType.TEXT_PLAIN).content("그냥 글자"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.message").value("지원하지 않는 형식입니다."));
  }

  // 로그인은 형식을 자세히 따지지 않음. 예전 규칙으로 가입한 회원도 들어와야 함
  @Test
  @DisplayName("로그인은 이메일 형식을 따지지 않음")
  void 로그인은_형식을_안_봄() throws Exception {
    when(authService.login(any(), anyString())).thenReturn(LoginResponse.of("a", "r", user()));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"옛날계정\",\"password\":\"a\"}"))
        .andExpect(status().isOk());

    verify(authService).login(any(), eq("127.0.0.1"));
  }
}
