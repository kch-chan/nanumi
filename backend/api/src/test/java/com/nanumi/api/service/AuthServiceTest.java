package com.nanumi.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanumi.api.dto.request.LoginRequest;
import com.nanumi.api.dto.request.RefreshRequest;
import com.nanumi.api.dto.request.SignupRequest;
import com.nanumi.api.dto.request.TermsAgreementRequest;
import com.nanumi.api.dto.request.WithdrawalRequest;
import com.nanumi.api.dto.response.LoginResponse;
import com.nanumi.api.dto.response.SignupResponse;
import com.nanumi.api.dto.response.TokenResponse;
import com.nanumi.api.entity.Account;
import com.nanumi.api.entity.RefreshToken;
import com.nanumi.api.entity.TermsAgreement;
import com.nanumi.api.entity.User;
import com.nanumi.api.exception.CustomException;
import com.nanumi.api.exception.ErrorCode;
import com.nanumi.api.repository.AccountRepository;
import com.nanumi.api.repository.RefreshTokenRepository;
import com.nanumi.api.repository.TermsAgreementRepository;
import com.nanumi.api.repository.UserRepository;
import com.nanumi.api.security.JwtTokenProvider;
import com.nanumi.api.security.JwtTokenProvider.TokenType;
import com.nanumi.api.security.LoginAttemptService;
import com.nanumi.api.security.SignupAttemptService;
import com.nanumi.api.security.password.NanumiPasswordEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

// 저장소·인코더·토큰 발급기를 전부 가짜로 두고 AuthService 의 판단만 확인함
// DB 도 스프링도 띄우지 않음
@ExtendWith(MockitoExtension.class)
@DisplayName("인증 서비스")
class AuthServiceTest {

  private static final String CLIENT_IP = "127.0.0.1";
  private static final String RAW_PASSWORD = "Ab3!efgh";
  private static final String STORED_HASH = "$nanumi$1$100000$salt$hash";

  // 프런트 constants/terms.ts 의 effectiveDate 가 그대로 올라옴
  private static final String TERMS_VERSION = "시행일자 2026년 8월";

  @Mock private UserRepository userRepository;
  @Mock private AccountRepository accountRepository;
  @Mock private NanumiPasswordEncoder passwordEncoder;
  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private LoginAttemptService loginAttemptService;
  @Mock private SignupAttemptService signupAttemptService;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private TermsAgreementRepository termsAgreementRepository;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService =
        new AuthService(
            userRepository,
            accountRepository,
            passwordEncoder,
            jwtTokenProvider,
            loginAttemptService,
            signupAttemptService,
            refreshTokenRepository,
            termsAgreementRepository);
  }

  // 로그인 응답 시간을 맞추려고 서비스가 미리 만들어 두는 해시임. 필요한 테스트에서만 부름
  private void initDummyHash() {
    when(passwordEncoder.encode(anyString())).thenReturn(STORED_HASH);
    authService.initDummyPasswordHash();
  }

  private User userWithId(int id) {
    User user = User.builder().nickname("나눔이").aptName("행복아파트").build();
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  private Account accountOf(User user) {
    Account account =
        Account.builder().user(user).email("nanumi@example.com").password(STORED_HASH).build();
    // 토큰 행이 계정 번호로 묶이므로 번호가 있어야 함
    ReflectionTestUtils.setField(account, "id", 7);
    return account;
  }

  // DB 에 담겨 있는 리프레시 토큰 행을 흉내 냄
  private RefreshToken storedToken(Account account, String rawToken, LocalDateTime expiresAt)
      throws Exception {
    return RefreshToken.builder()
        .account(account)
        .tokenHash(sha256Hex(rawToken))
        .expiresAt(expiresAt)
        .build();
  }

  private static String sha256Hex(String value) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
  }

  @Nested
  @DisplayName("회원가입")
  class Signup {

    private SignupRequest request(String dong, String ho) {
      return request(dong, ho, allAgreed());
    }

    private SignupRequest request(String dong, String ho, List<TermsAgreementRequest> agreements) {
      return new SignupRequest(
          "Nanumi@Example.com", RAW_PASSWORD, "나눔이", "행복아파트", dong, ho, agreements);
    }

    // 필수 둘에 동의하고 선택 하나는 거부한, 평범한 가입 화면의 결과임
    private List<TermsAgreementRequest> allAgreed() {
      return List.of(
          new TermsAgreementRequest("service", TERMS_VERSION, true),
          new TermsAgreementRequest("privacy", TERMS_VERSION, true),
          new TermsAgreementRequest("marketing", TERMS_VERSION, false));
    }

    @Test
    @DisplayName("이메일이 겹치면 409 를 던짐")
    void 이메일_중복() {
      when(accountRepository.existsByEmail("nanumi@example.com")).thenReturn(true);

      assertThatThrownBy(() -> authService.signup(request("101", "1502"), CLIENT_IP))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("닉네임이 겹치면 409 를 던짐")
    void 닉네임_중복() {
      when(accountRepository.existsByEmail(anyString())).thenReturn(false);
      when(userRepository.existsByNicknameIgnoreCase("나눔이")).thenReturn(true);

      assertThatThrownBy(() -> authService.signup(request("101", "1502"), CLIENT_IP))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
    }

    // 대소문자만 다른 주소로 또 가입하는 것을 막으려면 소문자로 맞춰서 찾고 저장해야 함
    @Test
    @DisplayName("이메일을 소문자로 맞춰서 저장함")
    void 이메일_소문자_정규화() {
      when(accountRepository.existsByEmail(anyString())).thenReturn(false);
      when(userRepository.existsByNicknameIgnoreCase(anyString())).thenReturn(false);
      when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(STORED_HASH);

      authService.signup(request("101", "1502"), CLIENT_IP);

      ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
      verify(accountRepository).save(captor.capture());
      assertThat(captor.getValue().getEmail()).isEqualTo("nanumi@example.com");
    }

    // 화면에서 비워 두면 빈 문자열이 오는데, 그대로 담으면 "값이 있는데 빈 값" 이 됨
    @Test
    @DisplayName("동·호를 비워서 보내면 null 로 담음")
    void 빈_동호는_null() {
      when(accountRepository.existsByEmail(anyString())).thenReturn(false);
      when(userRepository.existsByNicknameIgnoreCase(anyString())).thenReturn(false);
      when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(STORED_HASH);

      authService.signup(request("", "   "), CLIENT_IP);

      ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(captor.capture());
      assertThat(captor.getValue().getDong()).isNull();
      assertThat(captor.getValue().getHo()).isNull();
    }

    @Test
    @DisplayName("비밀번호는 해시해서 담고, 응답에 담지 않음")
    void 비밀번호는_해시해서_담음() {
      when(accountRepository.existsByEmail(anyString())).thenReturn(false);
      when(userRepository.existsByNicknameIgnoreCase(anyString())).thenReturn(false);
      when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(STORED_HASH);

      SignupResponse response = authService.signup(request("101", "1502"), CLIENT_IP);

      ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
      verify(accountRepository).save(captor.capture());
      assertThat(captor.getValue().getPassword()).isEqualTo(STORED_HASH);
      assertThat(response.user().nickname()).isEqualTo("나눔이");
    }

    // 화면의 "다음" 버튼 비활성화는 막는 장치가 아님. 서버가 다시 봐야 함
    @Test
    @DisplayName("필수 약관에 동의하지 않으면 가입을 막음")
    void 필수_약관_미동의() {
      List<TermsAgreementRequest> onlyMarketing =
          List.of(new TermsAgreementRequest("marketing", TERMS_VERSION, true));

      assertThatThrownBy(() -> authService.signup(request("101", "1502", onlyMarketing), CLIENT_IP))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.TERMS_NOT_AGREED);

      verify(userRepository, never()).save(any());
      verify(termsAgreementRepository, never()).save(any());
    }

    // 필수 약관을 보내기는 했지만 agreed = false 인 경우임
    @Test
    @DisplayName("필수 약관을 거부로 보내도 가입을 막음")
    void 필수_약관_거부() {
      List<TermsAgreementRequest> refused =
          List.of(
              new TermsAgreementRequest("service", TERMS_VERSION, true),
              new TermsAgreementRequest("privacy", TERMS_VERSION, false));

      assertThatThrownBy(() -> authService.signup(request("101", "1502", refused), CLIENT_IP))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.TERMS_NOT_AGREED);
    }

    // 동의 확인을 이메일 조회보다 앞에 둔 이유임
    // 동의 없이 보낸 요청으로는 가입된 이메일인지 알아낼 수 없어야 함
    @Test
    @DisplayName("약관 동의가 없으면 이메일 중복 여부를 알려 주지 않음")
    void 약관_확인이_중복_조회보다_먼저() {
      List<TermsAgreementRequest> none = List.of();

      assertThatThrownBy(() -> authService.signup(request("101", "1502", none), CLIENT_IP))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.TERMS_NOT_AGREED);

      verify(accountRepository, never()).existsByEmail(anyString());
    }

    @Test
    @DisplayName("동의 기록을 약관마다 한 줄씩 남김. 선택 약관의 거부도 남김")
    void 동의_기록_저장() {
      when(accountRepository.existsByEmail(anyString())).thenReturn(false);
      when(userRepository.existsByNicknameIgnoreCase(anyString())).thenReturn(false);
      when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(STORED_HASH);

      authService.signup(request("101", "1502"), CLIENT_IP);

      ArgumentCaptor<TermsAgreement> captor = ArgumentCaptor.forClass(TermsAgreement.class);
      verify(termsAgreementRepository, times(3)).save(captor.capture());

      assertThat(captor.getAllValues())
          .extracting(TermsAgreement::getTermsKey, TermsAgreement::isAgreed)
          .containsExactlyInAnyOrder(
              tuple("service", true), tuple("privacy", true), tuple("marketing", false));

      assertThat(captor.getAllValues())
          .allSatisfy(
              agreement -> {
                assertThat(agreement.getTermsVersion()).isEqualTo(TERMS_VERSION);
                assertThat(agreement.getAgreedAt()).isNotNull();
              });
    }

    // 프런트가 약관을 추가하고 서버가 아직 모르는 상태에서 가입 전체가 막히면 안 됨
    @Test
    @DisplayName("서버가 모르는 약관 key 는 버리고 나머지는 처리함")
    void 모르는_약관은_무시() {
      when(accountRepository.existsByEmail(anyString())).thenReturn(false);
      when(userRepository.existsByNicknameIgnoreCase(anyString())).thenReturn(false);
      when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(STORED_HASH);

      List<TermsAgreementRequest> withUnknown =
          List.of(
              new TermsAgreementRequest("service", TERMS_VERSION, true),
              new TermsAgreementRequest("privacy", TERMS_VERSION, true),
              new TermsAgreementRequest("아직-없는-약관", TERMS_VERSION, true));

      authService.signup(request("101", "1502", withUnknown), CLIENT_IP);

      ArgumentCaptor<TermsAgreement> captor = ArgumentCaptor.forClass(TermsAgreement.class);
      verify(termsAgreementRepository, times(2)).save(captor.capture());
      assertThat(captor.getAllValues())
          .extracting(TermsAgreement::getTermsKey)
          .containsExactlyInAnyOrder("service", "privacy");
    }

    // agreed 를 아예 안 보낸 경우임. Boolean 이라 null 로 들어옴
    @Test
    @DisplayName("agreed 를 빼고 보내면 동의하지 않은 것으로 봄")
    void agreed_누락은_미동의() {
      List<TermsAgreementRequest> missingFlag =
          List.of(
              new TermsAgreementRequest("service", TERMS_VERSION, null),
              new TermsAgreementRequest("privacy", TERMS_VERSION, true));

      assertThatThrownBy(() -> authService.signup(request("101", "1502", missingFlag), CLIENT_IP))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.TERMS_NOT_AGREED);
    }
  }

  @Nested
  @DisplayName("로그인")
  class Login {

    private final LoginRequest request = new LoginRequest("Nanumi@Example.com", RAW_PASSWORD);

    @Test
    @DisplayName("잠겨 있으면 비밀번호를 보기도 전에 끊음")
    void 잠긴_계정() {
      doThrow(new CustomException(ErrorCode.LOGIN_ATTEMPT_EXCEEDED))
          .when(loginAttemptService)
          .checkBlocked(anyString(), anyString());

      assertThatThrownBy(() -> authService.login(request, CLIENT_IP))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.LOGIN_ATTEMPT_EXCEEDED);

      verify(accountRepository, never()).findByEmail(anyString());
    }

    // 계정이 없을 때 바로 돌려주면 응답이 눈에 띄게 빨라져서 가입 여부가 드러남
    @Test
    @DisplayName("계정이 없어도 해싱을 한 번 돌리고 같은 오류를 냄")
    void 없는_계정도_해싱함() {
      initDummyHash();
      when(accountRepository.findByEmail("nanumi@example.com")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> authService.login(request, CLIENT_IP))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

      verify(passwordEncoder).matches(RAW_PASSWORD, STORED_HASH);
      verify(loginAttemptService).recordFailure("nanumi@example.com", CLIENT_IP);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 실패로 세고 같은 오류를 냄")
    void 비밀번호_불일치() {
      Account account = accountOf(userWithId(1));
      when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
      when(passwordEncoder.matches(RAW_PASSWORD, STORED_HASH)).thenReturn(false);

      assertThatThrownBy(() -> authService.login(request, CLIENT_IP))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

      verify(loginAttemptService).recordFailure("nanumi@example.com", CLIENT_IP);
      verify(loginAttemptService, never()).recordSuccess(anyString(), anyString());
    }

    @Test
    @DisplayName("탈퇴한 계정이면 거부함")
    void 탈퇴한_계정() {
      User user = userWithId(1);
      user.withdraw("그냥");
      when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(accountOf(user)));
      when(passwordEncoder.matches(RAW_PASSWORD, STORED_HASH)).thenReturn(true);

      assertThatThrownBy(() -> authService.login(request, CLIENT_IP))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.WITHDRAWN_USER);
    }

    @Test
    @DisplayName("성공하면 토큰 두 개를 내주고 실패 기록을 지움")
    void 로그인_성공() {
      Account account = accountOf(userWithId(1));
      when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
      when(passwordEncoder.matches(RAW_PASSWORD, STORED_HASH)).thenReturn(true);
      when(passwordEncoder.upgradeEncoding(STORED_HASH)).thenReturn(false);
      when(jwtTokenProvider.createAccessToken(1)).thenReturn("access-token");
      when(jwtTokenProvider.createRefreshToken(1)).thenReturn("refresh-token");
      when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(1_209_600_000L);

      LoginResponse response = authService.login(request, CLIENT_IP);

      assertThat(response.accessToken()).isEqualTo("access-token");
      assertThat(response.refreshToken()).isEqualTo("refresh-token");
      verify(loginAttemptService).recordSuccess("nanumi@example.com", CLIENT_IP);
    }

    // DB 에는 토큰 원문이 아니라 SHA-256 해시만 담겨야 함
    @Test
    @DisplayName("리프레시 토큰은 해시해서 담음")
    void 리프레시_토큰은_해시로_담김() throws Exception {
      Account account = accountOf(userWithId(1));
      when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
      when(passwordEncoder.matches(RAW_PASSWORD, STORED_HASH)).thenReturn(true);
      when(passwordEncoder.upgradeEncoding(STORED_HASH)).thenReturn(false);
      when(jwtTokenProvider.createAccessToken(1)).thenReturn("access-token");
      when(jwtTokenProvider.createRefreshToken(1)).thenReturn("refresh-token");
      when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(1_209_600_000L);

      authService.login(request, CLIENT_IP);

      ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
      verify(refreshTokenRepository).save(captor.capture());
      assertThat(captor.getValue().getTokenHash())
          .isEqualTo(sha256Hex("refresh-token"))
          .isNotEqualTo("refresh-token")
          .hasSize(64);
    }

    // 평문 비밀번호를 알 수 있는 자리는 로그인뿐이라 여기서 다시 해싱해 둠
    @Test
    @DisplayName("옛 방식 해시면 로그인할 때 새로 해싱해서 갱신함")
    void 옛_해시는_갱신됨() {
      Account account = accountOf(userWithId(1));
      when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
      when(passwordEncoder.matches(RAW_PASSWORD, STORED_HASH)).thenReturn(true);
      when(passwordEncoder.upgradeEncoding(STORED_HASH)).thenReturn(true);
      when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn("$nanumi$1$210000$new$hash");
      when(jwtTokenProvider.createAccessToken(1)).thenReturn("access-token");
      when(jwtTokenProvider.createRefreshToken(1)).thenReturn("refresh-token");
      when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(1_209_600_000L);

      authService.login(request, CLIENT_IP);

      assertThat(account.getPassword()).isEqualTo("$nanumi$1$210000$new$hash");
    }
  }

  @Nested
  @DisplayName("토큰 재발급")
  class Refresh {

    @Test
    @DisplayName("서명이 틀린 토큰이면 거부함")
    void 잘못된_토큰() {
      when(jwtTokenProvider.resolveUserId("bad", TokenType.REFRESH)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> authService.refresh(new RefreshRequest("bad")))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("계정을 못 찾으면 404 를 던짐")
    void 계정_없음() {
      when(jwtTokenProvider.resolveUserId(anyString(), eq(TokenType.REFRESH)))
          .thenReturn(Optional.of(1));
      when(accountRepository.findByUser_Id(1)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> authService.refresh(new RefreshRequest("token")))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    // 기한이 지난 행은 지우고 만료로 알림
    @Test
    @DisplayName("담아 둔 토큰의 기한이 지났으면 만료로 처리하고 지움")
    void 담아_둔_토큰_만료() throws Exception {
      Account account = accountOf(userWithId(1));
      RefreshToken expired = storedToken(account, "token", LocalDateTime.now().minusSeconds(1));
      when(jwtTokenProvider.resolveUserId(anyString(), eq(TokenType.REFRESH)))
          .thenReturn(Optional.of(1));
      when(accountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));
      when(refreshTokenRepository.findByTokenHash(sha256Hex("token")))
          .thenReturn(Optional.of(expired));

      assertThatThrownBy(() -> authService.refresh(new RefreshRequest("token")))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.EXPIRED_REFRESH_TOKEN);

      verify(refreshTokenRepository).delete(expired);
    }

    // 이미 한 번 회전돼서 버려진 토큰을 뒤늦게 쓰는 상황일 수 있음
    // 진짜 주인이 쓰던 토큰까지 같이 끊고 다시 로그인하게 해야 함
    @Test
    @DisplayName("담아 둔 행이 없는 토큰이 오면 그 계정의 모든 기기를 끊음")
    void 재사용_감지() throws Exception {
      Account account = accountOf(userWithId(1));
      when(jwtTokenProvider.resolveUserId("다른-토큰", TokenType.REFRESH)).thenReturn(Optional.of(1));
      when(accountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));
      when(refreshTokenRepository.findByTokenHash(sha256Hex("다른-토큰"))).thenReturn(Optional.empty());

      assertThatThrownBy(() -> authService.refresh(new RefreshRequest("다른-토큰")))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_TOKEN);

      verify(refreshTokenRepository).deleteByAccount_Id(account.getId());
    }

    @Test
    @DisplayName("성공하면 쓰인 토큰을 버리고 새 토큰을 담음")
    void 재발급_성공() throws Exception {
      Account account = accountOf(userWithId(1));
      RefreshToken old = storedToken(account, "옛-토큰", LocalDateTime.now().plusDays(1));
      when(jwtTokenProvider.resolveUserId("옛-토큰", TokenType.REFRESH)).thenReturn(Optional.of(1));
      when(accountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));
      when(refreshTokenRepository.findByTokenHash(sha256Hex("옛-토큰"))).thenReturn(Optional.of(old));
      when(jwtTokenProvider.createAccessToken(1)).thenReturn("새-액세스");
      when(jwtTokenProvider.createRefreshToken(1)).thenReturn("새-리프레시");
      when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(1_209_600_000L);

      TokenResponse response = authService.refresh(new RefreshRequest("옛-토큰"));

      assertThat(response.accessToken()).isEqualTo("새-액세스");
      assertThat(response.refreshToken()).isEqualTo("새-리프레시");

      // 쓰인 행은 버림(회전)
      verify(refreshTokenRepository).delete(old);

      ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
      verify(refreshTokenRepository).save(captor.capture());
      assertThat(captor.getValue().getTokenHash()).isEqualTo(sha256Hex("새-리프레시"));
    }

    // 기기마다 행이 따로 있어서, 다른 기기에서 로그인해도 이 기기의 토큰은 살아 있어야 함
    @Test
    @DisplayName("다른 기기의 토큰은 건드리지 않음")
    void 다른_기기_토큰_유지() throws Exception {
      Account account = accountOf(userWithId(1));
      RefreshToken phone = storedToken(account, "폰-토큰", LocalDateTime.now().plusDays(1));
      when(jwtTokenProvider.resolveUserId("폰-토큰", TokenType.REFRESH)).thenReturn(Optional.of(1));
      when(accountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));
      when(refreshTokenRepository.findByTokenHash(sha256Hex("폰-토큰")))
          .thenReturn(Optional.of(phone));
      when(jwtTokenProvider.createAccessToken(1)).thenReturn("새-액세스");
      when(jwtTokenProvider.createRefreshToken(1)).thenReturn("새-리프레시");
      when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(1_209_600_000L);

      authService.refresh(new RefreshRequest("폰-토큰"));

      // 계정 단위로 싹 지우는 일은 재사용이 감지됐을 때만 해야 함
      verify(refreshTokenRepository, never()).deleteByAccount_Id(account.getId());
    }
  }

  @Nested
  @DisplayName("로그아웃과 탈퇴")
  class LogoutAndWithdraw {

    // 토큰을 안 보내면 이 계정의 모든 기기를 끊음
    @Test
    @DisplayName("토큰 없이 로그아웃하면 모든 기기를 끊음")
    void 로그아웃_전체() {
      Account account = accountOf(userWithId(1));
      when(accountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));

      authService.logout(1, null);

      verify(refreshTokenRepository).deleteByAccount_Id(account.getId());
    }

    @Test
    @DisplayName("토큰을 보내면 그 기기만 끊음")
    void 로그아웃_기기_하나() throws Exception {
      Account account = accountOf(userWithId(1));
      RefreshToken phone = storedToken(account, "폰-토큰", LocalDateTime.now().plusDays(1));
      when(accountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));
      when(refreshTokenRepository.findByTokenHash(sha256Hex("폰-토큰")))
          .thenReturn(Optional.of(phone));

      authService.logout(1, "폰-토큰");

      verify(refreshTokenRepository).delete(phone);
      verify(refreshTokenRepository, never()).deleteByAccount_Id(account.getId());
    }

    @Test
    @DisplayName("탈퇴할 때 비밀번호가 틀리면 거부함")
    void 탈퇴_비밀번호_불일치() {
      Account account = accountOf(userWithId(1));
      when(accountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));
      when(passwordEncoder.matches(RAW_PASSWORD, STORED_HASH)).thenReturn(false);

      assertThatThrownBy(() -> authService.withdraw(1, new WithdrawalRequest(RAW_PASSWORD, "사유")))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("탈퇴하면 상태가 바뀌고 리프레시 토큰도 지워짐")
    void 탈퇴_성공() {
      User user = userWithId(1);
      Account account = accountOf(user);
      when(accountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));
      when(passwordEncoder.matches(RAW_PASSWORD, STORED_HASH)).thenReturn(true);

      authService.withdraw(1, new WithdrawalRequest(RAW_PASSWORD, "이사 갑니다"));

      assertThat(user.isWithdrawn()).isTrue();
      assertThat(user.getWithdrawalReason()).isEqualTo("이사 갑니다");
      // 탈퇴는 기기를 가리지 않고 전부 끊음
      verify(refreshTokenRepository).deleteByAccount_Id(account.getId());
    }

    @Test
    @DisplayName("이미 탈퇴한 계정이면 거부함")
    void 이미_탈퇴함() {
      User user = userWithId(1);
      user.withdraw(null);
      when(accountRepository.findByUser_Id(1)).thenReturn(Optional.of(accountOf(user)));

      assertThatThrownBy(() -> authService.withdraw(1, new WithdrawalRequest(RAW_PASSWORD, null)))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.WITHDRAWN_USER);
    }
  }
}
