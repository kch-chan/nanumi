package com.nanumi.api.controller;

import com.nanumi.api.dto.request.LoginRequest;
import com.nanumi.api.dto.request.LogoutRequest;
import com.nanumi.api.dto.request.RefreshRequest;
import com.nanumi.api.dto.request.SignupRequest;
import com.nanumi.api.dto.request.WithdrawalRequest;
import com.nanumi.api.dto.response.LoginResponse;
import com.nanumi.api.dto.response.LogoutResponse;
import com.nanumi.api.dto.response.SignupResponse;
import com.nanumi.api.dto.response.TokenResponse;
import com.nanumi.api.dto.response.WithdrawalResponse;
import com.nanumi.api.security.ClientIpResolver;
import com.nanumi.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final ClientIpResolver clientIpResolver;

  @PostMapping("/signup")
  public ResponseEntity<SignupResponse> signup(
      @Valid @RequestBody SignupRequest request, HttpServletRequest servletRequest) {
    // 가입 횟수를 IP 로 세기 때문에 IP 를 같이 넘김
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(authService.signup(request, resolveClientIp(servletRequest)));
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
    // 실패 횟수를 이메일과 접속 IP 로 따로 세기 때문에 IP 를 같이 넘김
    return ResponseEntity.ok(authService.login(request, resolveClientIp(servletRequest)));
  }

  @PostMapping("/refresh")
  public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    return ResponseEntity.ok(authService.refresh(request));
  }

  // 몸통은 선택임. 리프레시 토큰을 담아 보내면 그 기기만 로그아웃하고,
  // 없으면 이 계정의 모든 기기를 로그아웃함
  @PostMapping("/logout")
  public ResponseEntity<LogoutResponse> logout(
      @AuthenticationPrincipal Integer userId,
      @Valid @RequestBody(required = false) LogoutRequest request) {
    return ResponseEntity.ok(
        authService.logout(userId, request == null ? null : request.refreshToken()));
  }

  @PostMapping("/withdrawal")
  public ResponseEntity<WithdrawalResponse> withdraw(
      @AuthenticationPrincipal Integer userId, @Valid @RequestBody WithdrawalRequest request) {
    return ResponseEntity.ok(authService.withdraw(userId, request));
  }

  // 어느 값을 접속자 IP 로 볼지는 ClientIpResolver 가 정함
  // 프록시 뒤에서는 연결 주소가 프록시 것이라, 그대로 쓰면 모든 이용자가 한 열쇠를 공유하게 됨
  private String resolveClientIp(HttpServletRequest request) {
    return clientIpResolver.resolve(request);
  }
}
