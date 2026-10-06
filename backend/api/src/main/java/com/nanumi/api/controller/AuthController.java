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
import com.nanumi.api.service.AuthService;
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

  @PostMapping("/signup")
  public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
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
}
