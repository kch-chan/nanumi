package com.nanumi.api.service;

import com.nanumi.api.dto.request.LoginRequest;
import com.nanumi.api.dto.request.SignupRequest;
import com.nanumi.api.dto.response.LoginResponse;
import com.nanumi.api.dto.response.SignupResponse;
import com.nanumi.api.dto.response.UserResponse;
import com.nanumi.api.entity.AccountEntity;
import com.nanumi.api.entity.UserEntity;
import com.nanumi.api.exception.CustomException;
import com.nanumi.api.exception.ErrorCode;
import com.nanumi.api.repository.AccountEntityRepository;
import com.nanumi.api.repository.UserEntityRepository;
import com.nanumi.api.security.JwtTokenProvider;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

  private final UserEntityRepository userEntityRepository;
  private final AccountEntityRepository accountEntityRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  public SignupResponse signup(SignupRequest request) {
    if (accountEntityRepository.existsByEmail(request.email())) {
      throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
    }
    if (userEntityRepository.existsByNickname(request.nickname())) {
      throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
    }

    UserEntity user =
        UserEntity.builder()
            .nickname(request.nickname())
            .aptName(request.aptName())
            .dong(request.dong())
            .ho(request.ho())
            .build();
    userEntityRepository.save(user);

    AccountEntity account =
        AccountEntity.builder()
            .user(user)
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .build();
    accountEntityRepository.save(account);

    return SignupResponse.of(UserResponse.from(user));
  }

  public LoginResponse login(LoginRequest request) {
    AccountEntity account =
        accountEntityRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

    if (!passwordEncoder.matches(request.password(), account.getPassword())) {
      throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
    }

    UserEntity user = account.getUser();
    String accessToken = jwtTokenProvider.createAccessToken(user.getId());
    String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

    account.updateRefreshToken(
        refreshToken,
        LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000));

    return LoginResponse.of(accessToken, refreshToken, UserResponse.from(user));
  }
}
