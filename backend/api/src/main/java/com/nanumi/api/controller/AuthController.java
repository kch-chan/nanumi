package com.nanumi.api.controller;

import com.nanumi.api.dto.request.LoginRequest;
import com.nanumi.api.dto.request.SignupRequest;
import com.nanumi.api.dto.request.WithdrawalRequest;
import com.nanumi.api.dto.response.LoginResponse;
import com.nanumi.api.dto.response.LogoutResponse;
import com.nanumi.api.dto.response.SignupResponse;
import com.nanumi.api.dto.response.WithdrawalResponse;
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

  private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

  private final AuthService authService;

  @PostMapping("/signup")
  public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
    // 실패 횟수를 이메일 + 접속 IP 로 세기 때문에 IP 를 같이 넘김
    return ResponseEntity.ok(authService.login(request, resolveClientIp(servletRequest)));
  }

  @PostMapping("/logout")
  public ResponseEntity<LogoutResponse> logout(@AuthenticationPrincipal Long userId) {
    return ResponseEntity.ok(authService.logout(userId));
  }

  @PostMapping("/withdrawal")
  public ResponseEntity<WithdrawalResponse> withdraw(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody WithdrawalRequest request) {
    return ResponseEntity.ok(authService.withdraw(userId, request));
  }

  // 프록시를 거치면 실제 접속 IP 가 X-Forwarded-For 맨 앞에 들어감
  // 이 헤더는 요청하는 쪽에서 지어낼 수도 있으므로, 믿을 수 있는 프록시 뒤에서만 의미가 있음
  private String resolveClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader(FORWARDED_FOR_HEADER);
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
