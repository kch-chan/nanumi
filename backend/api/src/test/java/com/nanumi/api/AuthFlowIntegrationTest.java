package com.nanumi.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nanumi.api.entity.Account;
import com.nanumi.api.entity.User;
import com.nanumi.api.repository.AccountRepository;
import com.nanumi.api.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// 인증 흐름을 실제로 한 바퀴 돌려 보는 테스트임
//
// 단위 테스트로는 안 잡히는 것들을 여기서 잡음
//  - 리프레시 토큰으로 API 를 부를 수 있는지 (typ 클레임)
//  - 로그아웃·탈퇴 뒤에 토큰이 정말 끊기는지
//  - 잘못된 메서드나 경로가 500 이 아니라 제 상태 코드로 나가는지
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("인증 흐름")
class AuthFlowIntegrationTest {

  private static final String PASSWORD = "nanumi1234!";

  @Autowired private MockMvc mockMvc;
  @Autowired private AccountRepository accountRepository;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  void cleanUp() {
    accountRepository.deleteAll();
    userRepository.deleteAll();
  }

  // ---------- 회원가입 ----------

  @Test
  @DisplayName("가입하면 201 과 회원 정보가 나옴")
  void 회원가입() throws Exception {
    mockMvc
        .perform(json(post("/api/auth/signup"), signupBody("nanumi@example.com", "나눔이")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.user.nickname").value("나눔이"));
  }

  @Test
  @DisplayName("같은 이메일로 또 가입하면 409 임")
  void 이메일_중복() throws Exception {
    signup("nanumi@example.com", "나눔이");

    mockMvc
        .perform(json(post("/api/auth/signup"), signupBody("nanumi@example.com", "다른사람")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
  }

  @Test
  @DisplayName("이메일 대소문자만 바꿔도 같은 계정으로 봄")
  void 이메일_대소문자_중복() throws Exception {
    signup("nanumi@example.com", "나눔이");

    mockMvc
        .perform(json(post("/api/auth/signup"), signupBody("Nanumi@Example.com", "다른사람")))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("닉네임 대소문자만 바꿔도 중복으로 봄")
  void 닉네임_대소문자_중복() throws Exception {
    signup("first@example.com", "nanumi");

    mockMvc
        .perform(json(post("/api/auth/signup"), signupBody("second@example.com", "NANUMI")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));
  }

  // 화면에서 동·호를 비워 두면 빈 문자열로 오는데, 그대로 담으면 마이페이지에서 빈칸이 됨
  @Test
  @DisplayName("동·호를 비워서 보내면 null 로 담김")
  void 빈_동호는_null() throws Exception {
    mockMvc
        .perform(
            json(
                post("/api/auth/signup"),
                """
                {"email":"nanumi@example.com","password":"%s","nickname":"나눔이",
                 "aptName":"행복아파트","dong":"","ho":"  "}
                """
                    .formatted(PASSWORD)))
        .andExpect(status().isCreated());

    User user = userRepository.findByNickname("나눔이").orElseThrow();
    assertThat(user.getDong()).isNull();
    assertThat(user.getHo()).isNull();
  }

  // ---------- 로그인과 토큰 ----------

  @Test
  @DisplayName("로그인하면 액세스 토큰과 리프레시 토큰이 같이 나옴")
  void 로그인() throws Exception {
    signup("nanumi@example.com", "나눔이");

    mockMvc
        .perform(json(post("/api/auth/login"), loginBody("nanumi@example.com", PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty());
  }

  @Test
  @DisplayName("비밀번호가 틀리면 401 임")
  void 로그인_실패() throws Exception {
    signup("nanumi@example.com", "나눔이");

    mockMvc
        .perform(json(post("/api/auth/login"), loginBody("nanumi@example.com", "wrong1234!")))
        .andExpect(status().isUnauthorized());
  }

  // 액세스 토큰과 리프레시 토큰의 내용이 같으면 리프레시 토큰이 14일짜리 액세스 토큰이 되어 버림
  @Test
  @DisplayName("리프레시 토큰으로는 보호된 API 를 부를 수 없음")
  void 리프레시_토큰으로는_API를_못_부름() throws Exception {
    signup("nanumi@example.com", "나눔이");
    String[] tokens = login("nanumi@example.com");

    mockMvc
        .perform(post("/api/auth/logout").header("Authorization", "Bearer " + tokens[1]))
        .andExpect(status().isUnauthorized());

    // 액세스 토큰으로는 정상 동작함
    mockMvc
        .perform(post("/api/auth/logout").header("Authorization", "Bearer " + tokens[0]))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("리프레시하면 새 토큰이 나오고, 예전 리프레시 토큰은 막힘")
  void 리프레시_회전() throws Exception {
    signup("nanumi@example.com", "나눔이");
    String oldRefreshToken = login("nanumi@example.com")[1];

    String body =
        mockMvc
            .perform(json(post("/api/auth/refresh"), refreshBody(oldRefreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    String newRefreshToken = JsonPath.read(body, "$.refreshToken");
    assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

    // 이미 회전돼서 버려진 토큰임. 훔친 토큰을 뒤늦게 쓰는 경우일 수 있으므로 세션째로 끊음
    mockMvc
        .perform(json(post("/api/auth/refresh"), refreshBody(oldRefreshToken)))
        .andExpect(status().isUnauthorized());

    // 방금 받은 토큰까지 같이 끊겼는지 확인함
    mockMvc
        .perform(json(post("/api/auth/refresh"), refreshBody(newRefreshToken)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("로그아웃하면 리프레시도 막힘")
  void 로그아웃하면_리프레시_불가() throws Exception {
    signup("nanumi@example.com", "나눔이");
    String[] tokens = login("nanumi@example.com");

    mockMvc
        .perform(post("/api/auth/logout").header("Authorization", "Bearer " + tokens[0]))
        .andExpect(status().isOk());

    mockMvc
        .perform(json(post("/api/auth/refresh"), refreshBody(tokens[1])))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("만료된 리프레시 토큰입니다."));
  }

  // ---------- 탈퇴 ----------

  @Test
  @DisplayName("탈퇴하면 사유가 담기고 다시 로그인할 수 없음")
  void 탈퇴() throws Exception {
    signup("nanumi@example.com", "나눔이");
    String[] tokens = login("nanumi@example.com");

    mockMvc
        .perform(
            json(
                    post("/api/auth/withdrawal"),
                    """
                    {"password":"%s","reason":"이사 갑니다"}
                    """
                        .formatted(PASSWORD))
                .header("Authorization", "Bearer " + tokens[0]))
        .andExpect(status().isOk());

    User user = userRepository.findByNickname("나눔이").orElseThrow();
    assertThat(user.isWithdrawn()).isTrue();
    assertThat(user.getWithdrawalReason()).isEqualTo("이사 갑니다");

    Account account = accountRepository.findByUser_Id(user.getId()).orElseThrow();
    assertThat(account.hasRefreshToken()).isFalse();

    mockMvc
        .perform(json(post("/api/auth/login"), loginBody("nanumi@example.com", PASSWORD)))
        .andExpect(status().isForbidden());
  }

  // ---------- 잠금과 오류 응답 ----------

  @Test
  @DisplayName("한 계정으로 5회 실패하면 429 로 막힘")
  void 로그인_잠금() throws Exception {
    signup("locked@example.com", "잠긴사람");

    for (int i = 0; i < 5; i++) {
      mockMvc
          .perform(json(post("/api/auth/login"), loginBody("locked@example.com", "wrong1234!")))
          .andExpect(status().isUnauthorized());
    }

    // 이제는 비밀번호가 맞아도 막힘
    mockMvc
        .perform(json(post("/api/auth/login"), loginBody("locked@example.com", PASSWORD)))
        .andExpect(status().isTooManyRequests());
  }

  // 예전에는 ResponseEntityExceptionHandler 를 물려받지 않아서 이런 요청이 전부 500 으로 나갔음
  @Test
  @DisplayName("GET 으로 로그인을 부르면 405 임")
  void 잘못된_메서드() throws Exception {
    mockMvc.perform(get("/api/auth/login")).andExpect(status().isMethodNotAllowed());
  }

  @Test
  @DisplayName("본문 JSON 이 깨졌으면 400 임")
  void 깨진_JSON() throws Exception {
    mockMvc
        .perform(json(post("/api/auth/login"), "{\"email\": "))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("토큰 없이 보호된 API 를 부르면 401 이고 응답 모양이 같음")
  void 토큰_없이_호출() throws Exception {
    mockMvc
        .perform(post("/api/auth/logout"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value("유효하지 않은 토큰입니다."));
  }

  @Test
  @DisplayName("검증에 걸리면 어느 규칙인지 알려 줌")
  void 검증_메시지() throws Exception {
    mockMvc
        .perform(
            json(
                post("/api/auth/signup"),
                """
                {"email":"나눔@example.com","password":"%s","nickname":"나눔이","aptName":"행복아파트"}
                """
                    .formatted(PASSWORD)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("이메일에는 영문, 숫자와 일부 기호만 사용할 수 있습니다."));
  }

  // ---------- 도우미 ----------

  private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
    return builder.contentType(MediaType.APPLICATION_JSON).content(body);
  }

  private String signupBody(String email, String nickname) {
    return """
        {"email":"%s","password":"%s","nickname":"%s","aptName":"행복아파트","dong":"101","ho":"1001"}
        """
        .formatted(email, PASSWORD, nickname);
  }

  private String loginBody(String email, String password) {
    return """
        {"email":"%s","password":"%s"}
        """
        .formatted(email, password);
  }

  private String refreshBody(String refreshToken) {
    return """
        {"refreshToken":"%s"}
        """
        .formatted(refreshToken);
  }

  private void signup(String email, String nickname) throws Exception {
    mockMvc
        .perform(json(post("/api/auth/signup"), signupBody(email, nickname)))
        .andExpect(status().isCreated());
  }

  // [0] 액세스 토큰, [1] 리프레시 토큰
  private String[] login(String email) throws Exception {
    String body =
        mockMvc
            .perform(json(post("/api/auth/login"), loginBody(email, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    return new String[] {
      JsonPath.read(body, "$.accessToken"), JsonPath.read(body, "$.refreshToken")
    };
  }
}
