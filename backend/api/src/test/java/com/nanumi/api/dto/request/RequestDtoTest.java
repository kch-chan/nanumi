package com.nanumi.api.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// 요청 DTO 는 record 라서 코드가 없고 제약 애노테이션만 있음.
// 그래서 "필드가 담긴다" 가 아니라 "어떤 값이 어떤 문구로 거절되는가" 를 확인함
//
// 컨트롤러 테스트(AuthControllerTest)는 상태 코드까지 보고, 여기서는 칸마다의 규칙을 봄.
// 애노테이션을 지우거나 길이를 바꾸면 컨트롤러 테스트는 통과해도 이쪽이 깨짐
@DisplayName("요청 DTO 제약")
class RequestDtoTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void beforeAll() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void afterAll() {
    factory.close();
  }

  private static List<String> messages(Object target) {
    Set<ConstraintViolation<Object>> violations = validator.validate(target);
    return violations.stream().map(ConstraintViolation::getMessage).toList();
  }

  private static List<TermsAgreementRequest> agreements() {
    return List.of(
        new TermsAgreementRequest("service", "시행일자 2026년 8월", true),
        new TermsAgreementRequest("privacy", "시행일자 2026년 8월", true),
        new TermsAgreementRequest("marketing", "시행일자 2026년 8월", false));
  }

  private static SignupRequest signup(String email, String password, String nickname) {
    return new SignupRequest(email, password, nickname, "행복아파트", "101", "1502", agreements());
  }

  @Nested
  @DisplayName("회원가입 요청")
  class SignupRequestTest {

    @Test
    @DisplayName("모든 칸이 올바르면 통과함")
    void 정상() {
      assertThat(messages(signup("nanumi@example.com", "Ab3!efgh", "나눔이"))).isEmpty();
    }

    @Test
    @DisplayName("동·호는 비워도 통과함")
    void 선택_항목() {
      SignupRequest request =
          new SignupRequest(
              "nanumi@example.com", "Ab3!efgh", "나눔이", "행복아파트", null, null, agreements());

      assertThat(messages(request)).isEmpty();
    }

    @Test
    @DisplayName("이메일 형식이 어긋나면 규칙별 문구로 알려 줌")
    void 이메일_위반() {
      assertThat(messages(signup("nanumi", "Ab3!efgh", "나눔이")))
          .containsExactly("이메일에는 @를 하나만 포함해야 합니다.");
    }

    @Test
    @DisplayName("비밀번호 구성이 모자라면 알려 줌")
    void 비밀번호_위반() {
      assertThat(messages(signup("nanumi@example.com", "abcdefgh", "나눔이")))
          .containsExactly("비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.");
    }

    @Test
    @DisplayName("닉네임 형식이 어긋나면 알려 줌")
    void 닉네임_위반() {
      assertThat(messages(signup("nanumi@example.com", "Ab3!efgh", "가")))
          .contains("닉네임 형식이 맞지 않습니다.");
    }

    // 닉네임·아파트명은 저장된 뒤 화면에 그대로 그려질 수 있는 값이라 태그를 받지 않음
    @Test
    @DisplayName("자유 입력 칸에 태그가 들어오면 거절함")
    void 태그_거절() {
      SignupRequest request =
          new SignupRequest(
              "nanumi@example.com", "Ab3!efgh", "나눔이", "<script>행복아파트", null, null, agreements());

      assertThat(messages(request)).contains("HTML 태그는 사용할 수 없습니다.");
    }

    @Test
    @DisplayName("아파트명이 비면 알려 줌")
    void 아파트명_필수() {
      SignupRequest request =
          new SignupRequest(
              "nanumi@example.com", "Ab3!efgh", "나눔이", "  ", null, null, agreements());

      assertThat(messages(request)).contains("아파트명을 입력해 주세요.");
    }

    @Test
    @DisplayName("동·호가 20자를 넘으면 알려 줌")
    void 동호_길이() {
      SignupRequest request =
          new SignupRequest(
              "nanumi@example.com",
              "Ab3!efgh",
              "나눔이",
              "행복아파트",
              "1".repeat(21),
              "2".repeat(21),
              agreements());

      assertThat(messages(request)).contains("동은 20자 이하여야 합니다.", "호는 20자 이하여야 합니다.");
    }

    // 동의를 받았다는 사실의 입증 책임이 사업자에게 있어 목록 자체를 필수로 둠
    @Test
    @DisplayName("약관 동의 목록이 비면 알려 줌")
    void 약관_목록_필수() {
      SignupRequest request =
          new SignupRequest(
              "nanumi@example.com", "Ab3!efgh", "나눔이", "행복아파트", null, null, List.of());

      assertThat(messages(request)).contains("약관 동의 정보가 필요합니다.");
    }

    // @Valid 가 붙어 있어야 목록 안쪽 요소의 제약까지 검사됨
    // 빠뜨리면 key 가 비어 있어도 그대로 통과해서 빈 문자열이 DB 로 들어감
    @Test
    @DisplayName("목록 안쪽 항목의 제약까지 검사함")
    void 약관_요소_검사() {
      SignupRequest request =
          new SignupRequest(
              "nanumi@example.com",
              "Ab3!efgh",
              "나눔이",
              "행복아파트",
              null,
              null,
              List.of(new TermsAgreementRequest("  ", "", true)));

      assertThat(messages(request)).contains("약관 종류가 필요합니다.", "약관 판이 필요합니다.");
    }
  }

  @Nested
  @DisplayName("약관 동의 항목")
  class TermsAgreementRequestTest {

    @Test
    @DisplayName("올바른 값은 통과함")
    void 정상() {
      assertThat(messages(new TermsAgreementRequest("service", "시행일자 2026년 8월", true))).isEmpty();
    }

    @Test
    @DisplayName("약관 종류가 20자를 넘으면 알려 줌")
    void 종류_길이() {
      assertThat(messages(new TermsAgreementRequest("s".repeat(21), "2026-08", true)))
          .contains("약관 종류가 올바르지 않습니다.");
    }

    @Test
    @DisplayName("약관 판이 40자를 넘으면 알려 줌")
    void 판_길이() {
      assertThat(messages(new TermsAgreementRequest("service", "v".repeat(41), true)))
          .contains("약관 판이 올바르지 않습니다.");
    }

    // agreed 를 원시 boolean 으로 두면 null 이 조용히 false 가 되어
    // "안 보냄" 과 "거부" 를 구분할 수 없음. 그래서 래퍼 타입으로 받고 여기서 판정함
    @Test
    @DisplayName("동의 여부를 안 보내면 동의하지 않은 것으로 봄")
    void 동의_생략() {
      assertThat(new TermsAgreementRequest("marketing", "2026-08", null).isAgreed()).isFalse();
      assertThat(new TermsAgreementRequest("marketing", "2026-08", false).isAgreed()).isFalse();
      assertThat(new TermsAgreementRequest("marketing", "2026-08", true).isAgreed()).isTrue();
    }
  }

  @Nested
  @DisplayName("로그인 요청")
  class LoginRequestTest {

    // 로그인은 형식을 자세히 따지지 않음
    // 옛 규칙으로 가입한 회원도 로그인돼야 하고, 형식 안내가 계정 탐색의 힌트가 되기 때문
    @Test
    @DisplayName("형식이 어긋난 값이어도 비어 있지만 않으면 통과함")
    void 형식_미검사() {
      assertThat(messages(new LoginRequest("nanumi", "1"))).isEmpty();
    }

    @Test
    @DisplayName("비어 있으면 알려 줌")
    void 빈_값() {
      assertThat(messages(new LoginRequest("  ", ""))).contains("이메일을 입력해 주세요.", "비밀번호를 입력해 주세요.");
    }

    // 상한이 없으면 수 MB 짜리 문자열 하나로 유니코드 정규화와 PBKDF2 10만 회를 그대로 돌리게 됨
    @Test
    @DisplayName("길이 상한을 넘으면 알려 줌")
    void 길이_상한() {
      assertThat(messages(new LoginRequest("a".repeat(101), "b".repeat(21))))
          .contains("이메일은 100자 이하여야 합니다.", "비밀번호는 20자 이하여야 합니다.");
    }
  }

  @Nested
  @DisplayName("토큰 재발급 요청")
  class RefreshRequestTest {

    @Test
    @DisplayName("토큰이 비면 알려 줌")
    void 빈_값() {
      assertThat(messages(new RefreshRequest("  "))).containsExactly("리프레시 토큰을 입력해 주세요.");
    }

    @Test
    @DisplayName("2000자를 넘으면 알려 줌")
    void 길이_상한() {
      assertThat(messages(new RefreshRequest("a".repeat(2001))))
          .containsExactly("리프레시 토큰이 올바르지 않습니다.");
    }
  }

  @Nested
  @DisplayName("로그아웃 요청")
  class LogoutRequestTest {

    // 몸통 자체가 선택임. 토큰을 보내면 그 기기만, 보내지 않으면 모든 기기를 로그아웃함
    @Test
    @DisplayName("토큰이 없어도 통과함")
    void 토큰_없음() {
      assertThat(messages(new LogoutRequest(null))).isEmpty();
    }

    @Test
    @DisplayName("2000자를 넘으면 알려 줌")
    void 길이_상한() {
      assertThat(messages(new LogoutRequest("a".repeat(2001))))
          .containsExactly("토큰 형식이 올바르지 않습니다.");
    }
  }

  @Nested
  @DisplayName("회원탈퇴 요청")
  class WithdrawalRequestTest {

    @Test
    @DisplayName("비밀번호만 있으면 통과함")
    void 정상() {
      assertThat(messages(new WithdrawalRequest("Ab3!efgh", null))).isEmpty();
    }

    // 본인 확인용이라 형식 검사를 걸지 않음. 옛 규칙으로 가입한 회원도 탈퇴할 수 있어야 함
    @Test
    @DisplayName("형식이 어긋난 비밀번호도 통과함")
    void 형식_미검사() {
      assertThat(messages(new WithdrawalRequest("1", null))).isEmpty();
    }

    @Test
    @DisplayName("비밀번호가 비면 알려 줌")
    void 비밀번호_필수() {
      assertThat(messages(new WithdrawalRequest("  ", null))).containsExactly("비밀번호를 입력해 주세요.");
    }

    @Test
    @DisplayName("비밀번호 길이 상한은 로그인과 같은 20자임")
    void 비밀번호_상한() {
      assertThat(messages(new WithdrawalRequest("a".repeat(21), null)))
          .containsExactly("비밀번호는 20자 이하여야 합니다.");
    }

    @Test
    @DisplayName("탈퇴 사유가 255자를 넘으면 알려 줌")
    void 사유_길이() {
      assertThat(messages(new WithdrawalRequest("Ab3!efgh", "가".repeat(256))))
          .containsExactly("탈퇴 사유는 255자 이하여야 합니다.");
    }

    @Test
    @DisplayName("탈퇴 사유에 스크립트 주소가 들어오면 거절함")
    void 사유_안전성() {
      assertThat(messages(new WithdrawalRequest("Ab3!efgh", "javascript:alert(1)")))
          .containsExactly("스크립트 주소는 사용할 수 없습니다.");
    }
  }
}
