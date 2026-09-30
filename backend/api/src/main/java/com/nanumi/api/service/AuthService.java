package com.nanumi.api.service;

import com.nanumi.api.dto.request.LoginRequest;
import com.nanumi.api.dto.request.RefreshRequest;
import com.nanumi.api.dto.request.SignupRequest;
import com.nanumi.api.dto.request.WithdrawalRequest;
import com.nanumi.api.dto.response.LoginResponse;
import com.nanumi.api.dto.response.LogoutResponse;
import com.nanumi.api.dto.response.SignupResponse;
import com.nanumi.api.dto.response.TokenResponse;
import com.nanumi.api.dto.response.UserResponse;
import com.nanumi.api.dto.response.WithdrawalResponse;
import com.nanumi.api.entity.Account;
import com.nanumi.api.entity.RefreshToken;
import com.nanumi.api.entity.User;
import com.nanumi.api.exception.CustomException;
import com.nanumi.api.exception.ErrorCode;
import com.nanumi.api.repository.AccountRepository;
import com.nanumi.api.repository.RefreshTokenRepository;
import com.nanumi.api.repository.UserRepository;
import com.nanumi.api.security.JwtTokenProvider;
import com.nanumi.api.security.JwtTokenProvider.TokenType;
import com.nanumi.api.security.LoginAttemptService;
import com.nanumi.api.security.SignupAttemptService;
import com.nanumi.api.security.password.NanumiPasswordEncoder;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

  private final UserRepository userEntityRepository;
  private final AccountRepository accountEntityRepository;
  private final NanumiPasswordEncoder nanumiPasswordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final LoginAttemptService loginAttemptService;
  private final SignupAttemptService signupAttemptService;
  private final RefreshTokenRepository refreshTokenRepository;

  // 한 계정이 동시에 유지할 수 있는 기기 수임
  // 상한이 없으면 로그인할 때마다 토큰 행이 하나씩 늘어나 끝없이 쌓임
  private static final int MAX_DEVICES_PER_ACCOUNT = 5;

  // 없는 계정으로 로그인을 시도해도 있을 때와 같은 시간을 쓰려고 미리 만들어 두는 해시임
  // 아무도 모르는 값으로 만들어서 이 해시에 맞는 비밀번호는 존재하지 않음
  //
  // 다만 이건 로그인 응답 시간만 맞추는 것임
  // 회원가입은 중복 이메일을 409 로 그대로 알려 주므로 가입 여부 자체를 숨기지는 못함
  // 가입 화면에서 "이미 쓰는 이메일" 안내를 빼면 UX 가 크게 나빠져서 노출을 감수한 것이고,
  // 로그인 쪽 방어는 "가입 여부를 모르는 사람이 응답 시간만으로 알아내는 것"을 막는 데 목적이 있음
  private String dummyPasswordHash;

  @PostConstruct
  void initDummyPasswordHash() {
    this.dummyPasswordHash = nanumiPasswordEncoder.encode(UUID.randomUUID().toString());
  }

  public SignupResponse signup(SignupRequest request, String clientIp) {
    // 가입 경로는 인증이 필요 없으므로 IP 단위로 횟수를 제한함
    // 막지 않으면 계정을 대량으로 만들거나, 409 응답만 보고 가입된 이메일을 훑을 수 있음
    signupAttemptService.checkBlocked(clientIp);
    signupAttemptService.recordAttempt(clientIp);

    String email = normalizeEmail(request.email());

    if (accountEntityRepository.existsByEmail(email)) {
      throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
    }

    // abc 와 ABC 가 따로 존재하면 사람이 헷갈리므로 대소문자를 무시하고 봄
    if (userEntityRepository.existsByNicknameIgnoreCase(request.nickname())) {
      throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
    }

    User user =
        User.builder()
            .nickname(request.nickname())
            .aptName(request.aptName())
            // 화면에서 비워 두면 빈 문자열로 오는데, 그대로 담으면 "값이 있는데 빈 값"이 됨
            .dong(blankToNull(request.dong()))
            .ho(blankToNull(request.ho()))
            .build();
    userEntityRepository.save(user);

    Account account =
        Account.builder()
            .user(user)
            .email(email)
            .password(nanumiPasswordEncoder.encode(request.password()))
            .build();
    accountEntityRepository.save(account);

    return SignupResponse.of(UserResponse.from(user));
  }

  public LoginResponse login(LoginRequest request, String clientIp) {
    String email = normalizeEmail(request.email());

    // 막혀 있으면 비밀번호를 맞춰 보기 전에 끊음
    loginAttemptService.checkBlocked(email, clientIp);

    Account account = accountEntityRepository.findByEmail(email).orElse(null);

    if (account == null) {
      // 여기서 바로 돌려주면 응답이 눈에 띄게 빨라져서 가입 여부가 드러남
      // 계정이 있을 때와 같은 만큼 해싱을 돌리고 똑같은 오류를 냄
      nanumiPasswordEncoder.matches(request.password(), dummyPasswordHash);
      loginAttemptService.recordFailure(email, clientIp);
      throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
    }

    if (!nanumiPasswordEncoder.matches(request.password(), account.getPassword())) {
      loginAttemptService.recordFailure(email, clientIp);
      throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
    }

    // 비밀번호는 맞았으므로 무차별 대입은 아님. 실패 기록을 지움
    loginAttemptService.recordSuccess(email, clientIp);

    User user = account.getUser();
    if (user.isWithdrawn()) {
      throw new CustomException(ErrorCode.WITHDRAWN_USER);
    }

    // 평문 비밀번호를 알 수 있는 자리는 여기뿐임
    // 예전 BCrypt 해시나 반복 횟수가 낮은 해시는 이 참에 새 파라미터로 다시 해싱해 둠
    if (nanumiPasswordEncoder.upgradeEncoding(account.getPassword())) {
      account.changePassword(nanumiPasswordEncoder.encode(request.password()));
    }

    String accessToken = jwtTokenProvider.createAccessToken(user.getId());
    String refreshToken = issueRefreshToken(account, user);

    return LoginResponse.of(accessToken, refreshToken, UserResponse.from(user));
  }

  // 리프레시 토큰으로 액세스 토큰을 다시 받음. 리프레시 토큰도 함께 새로 내줌(회전)
  //
  // 담아 둔 해시와 다른 토큰이 오면 이 계정의 세션을 통째로 끊는데,
  // 그 삭제는 커밋되어야 하므로 CustomException 에서는 롤백하지 않도록 해 둠
  // (이 메서드는 검사를 다 통과한 뒤에야 값을 바꾸므로 롤백하지 않아도 남는 부작용이 없음)
  @Transactional(noRollbackFor = CustomException.class)
  public TokenResponse refresh(RefreshRequest request) {
    Integer userId =
        jwtTokenProvider
            .resolveUserId(request.refreshToken(), TokenType.REFRESH)
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

    Account account =
        accountEntityRepository
            .findByUser_Id(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    RefreshToken stored =
        refreshTokenRepository
            .findByTokenHash(hashRefreshToken(request.refreshToken()))
            .orElse(null);

    if (stored == null) {
      // 서명은 맞는데 담아 둔 행이 없음
      // 이미 회전돼 버려진 토큰을 누군가 뒤늦게 쓰는 상황일 수 있으므로,
      // 이 계정의 다른 기기 토큰까지 같이 끊고 전부 다시 로그인하게 함
      refreshTokenRepository.deleteByAccount_Id(account.getId());
      throw new CustomException(ErrorCode.INVALID_TOKEN);
    }

    // 다른 계정의 토큰이 서명을 통과하는 일은 없지만, 확인해서 손해 볼 것이 없음
    if (stored.getAccount().getId() != account.getId()) {
      refreshTokenRepository.delete(stored);
      throw new CustomException(ErrorCode.INVALID_TOKEN);
    }

    if (stored.isExpired()) {
      refreshTokenRepository.delete(stored);
      throw new CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN);
    }

    User user = account.getUser();
    if (user.isWithdrawn()) {
      throw new CustomException(ErrorCode.WITHDRAWN_USER);
    }

    // 회전: 쓰인 토큰은 버리고 새로 하나 만듦. 다른 기기의 토큰은 그대로 둠
    refreshTokenRepository.delete(stored);

    String accessToken = jwtTokenProvider.createAccessToken(user.getId());
    String refreshToken = issueRefreshToken(account, user);

    return TokenResponse.of(accessToken, refreshToken);
  }

  // 리프레시 토큰을 같이 보내면 그 기기만 로그아웃함
  // 보내지 않으면 이 계정의 모든 기기를 로그아웃함(토큰을 잃어버렸을 때를 위한 길)
  public LogoutResponse logout(Integer userId, String refreshToken) {
    Account account =
        accountEntityRepository
            .findByUser_Id(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    if (refreshToken == null || refreshToken.isBlank()) {
      refreshTokenRepository.deleteByAccount_Id(account.getId());
      return LogoutResponse.of();
    }

    refreshTokenRepository
        .findByTokenHash(hashRefreshToken(refreshToken))
        // 남의 토큰을 지워 달라고 보내는 것을 막음
        .filter(stored -> stored.getAccount().getId() == account.getId())
        .ifPresent(refreshTokenRepository::delete);

    return LogoutResponse.of();
  }

  public WithdrawalResponse withdraw(Integer userId, WithdrawalRequest request) {
    Account account =
        accountEntityRepository
            .findByUser_Id(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    User user = account.getUser();
    if (user.isWithdrawn()) {
      throw new CustomException(ErrorCode.WITHDRAWN_USER);
    }

    if (!nanumiPasswordEncoder.matches(request.password(), account.getPassword())) {
      throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
    }

    // 탈퇴 사유는 개인정보 수집·이용 동의에 적어 둔 선택 항목이라 받은 값을 담아 둠
    user.withdraw(blankToNull(request.reason()));
    // 탈퇴는 기기를 가리지 않고 전부 끊음
    refreshTokenRepository.deleteByAccount_Id(account.getId());

    return WithdrawalResponse.of(user.getWithdrawnAt());
  }

  // 리프레시 토큰을 새로 만들고, DB 에는 해시만 담은 행을 하나 추가함
  private String issueRefreshToken(Account account, User user) {
    String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

    refreshTokenRepository.save(
        RefreshToken.builder()
            .account(account)
            .tokenHash(hashRefreshToken(refreshToken))
            .expiresAt(
                LocalDateTime.now()
                    .plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000))
            .build());

    pruneRefreshTokens(account);

    return refreshToken;
  }

  // 만료된 행을 치우고, 기기 수가 상한을 넘으면 오래된 것부터 지움
  private void pruneRefreshTokens(Account account) {
    List<RefreshToken> tokens =
        refreshTokenRepository.findByAccount_IdOrderByCreatedAtAsc(account.getId());

    tokens.stream().filter(RefreshToken::isExpired).forEach(refreshTokenRepository::delete);

    List<RefreshToken> alive = tokens.stream().filter(token -> !token.isExpired()).toList();
    int excess = alive.size() - MAX_DEVICES_PER_ACCOUNT;
    for (int i = 0; i < excess; i++) {
      refreshTokenRepository.delete(alive.get(i));
    }
  }

  // 토큰 원문 대신 SHA-256 16진수 64자를 담음
  // 토큰 자체가 이미 길고 추측할 수 없는 값이라 salt 없이 한 번만 돌려도 충분함
  private static String hashRefreshToken(String refreshToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 을 쓰지 못함", e);
    }
  }

  // 이메일은 대소문자를 가리지 않으므로 소문자로 맞춰서 저장하고 찾음
  // 이렇게 해야 Test@a.com 으로 가입한 뒤 test@a.com 으로 또 가입하는 걸 막을 수 있음
  private String normalizeEmail(String email) {
    return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
  }

  private String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }
}
