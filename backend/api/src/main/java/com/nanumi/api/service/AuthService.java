package com.nanumi.api.service;

import com.nanumi.api.dto.request.LoginRequest;
import com.nanumi.api.dto.request.SignupRequest;
import com.nanumi.api.dto.request.WithdrawalRequest;
import com.nanumi.api.dto.response.LoginResponse;
import com.nanumi.api.dto.response.LogoutResponse;
import com.nanumi.api.dto.response.SignupResponse;
import com.nanumi.api.dto.response.UserResponse;
import com.nanumi.api.dto.response.WithdrawalResponse;
import com.nanumi.api.entity.Account;
import com.nanumi.api.entity.User;
import com.nanumi.api.exception.CustomException;
import com.nanumi.api.exception.ErrorCode;
import com.nanumi.api.repository.AccountRepository;
import com.nanumi.api.repository.UserRepository;
import com.nanumi.api.security.JwtTokenProvider;
import com.nanumi.api.security.LoginAttemptService;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

  private final UserRepository userEntityRepository;
  private final AccountRepository accountEntityRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final LoginAttemptService loginAttemptService;

  // 없는 계정으로 로그인을 시도해도 있을 때와 같은 시간을 쓰려고 미리 만들어 두는 해시임
  // 아무도 모르는 값으로 만들어서 이 해시에 맞는 비밀번호는 존재하지 않음
  private String dummyPasswordHash;

  @PostConstruct
  void initDummyPasswordHash() {
    this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
  }

  public SignupResponse signup(SignupRequest request) {
    String email = normalizeEmail(request.email());

    if (accountEntityRepository.existsByEmail(email)) {
      throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
    }
    if (userEntityRepository.existsByNickname(request.nickname())) {
      throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
    }

    User user =
        User.builder()
            .nickname(request.nickname())
            .aptName(request.aptName())
            .dong(request.dong())
            .ho(request.ho())
            .build();
    userEntityRepository.save(user);

    Account account =
        Account.builder()
            .user(user)
            .email(email)
            .password(passwordEncoder.encode(request.password()))
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
      passwordEncoder.matches(request.password(), dummyPasswordHash);
      loginAttemptService.recordFailure(email, clientIp);
      throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
    }

    if (!passwordEncoder.matches(request.password(), account.getPassword())) {
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
    if (passwordEncoder.upgradeEncoding(account.getPassword())) {
      account.changePassword(passwordEncoder.encode(request.password()));
    }

    String accessToken = jwtTokenProvider.createAccessToken(user.getId());
    String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

    account.updateRefreshToken(
        refreshToken,
        LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000));

    return LoginResponse.of(accessToken, refreshToken, UserResponse.from(user));
  }

  public LogoutResponse logout(Long userId) {
    Account account =
        accountEntityRepository
            .findByUser_Id(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    account.clearRefreshToken();

    return LogoutResponse.of();
  }

  public WithdrawalResponse withdraw(Long userId, WithdrawalRequest request) {
    Account account =
        accountEntityRepository
            .findByUser_Id(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    User user = account.getUser();
    if (user.isWithdrawn()) {
      throw new CustomException(ErrorCode.WITHDRAWN_USER);
    }

    if (!passwordEncoder.matches(request.password(), account.getPassword())) {
      throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
    }

    user.withdraw();
    account.clearRefreshToken();

    return WithdrawalResponse.of(user.getWithdrawnAt());
  }

  // 이메일은 대소문자를 가리지 않으므로 소문자로 맞춰서 저장하고 찾음
  // 이렇게 해야 Test@a.com 으로 가입한 뒤 test@a.com 으로 또 가입하는 걸 막을 수 있음
  private String normalizeEmail(String email) {
    return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
  }
}
