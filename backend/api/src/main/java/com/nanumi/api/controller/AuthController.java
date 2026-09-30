package com.nanumi.api.controller;

import com.nanumi.api.dto.request.LoginRequest;
import com.nanumi.api.dto.request.RefreshRequest;
import com.nanumi.api.dto.request.SignupRequest;
import com.nanumi.api.dto.request.WithdrawalRequest;
import com.nanumi.api.dto.response.LoginResponse;
import com.nanumi.api.dto.response.LogoutResponse;
import com.nanumi.api.dto.response.SignupResponse;
import com.nanumi.api.dto.response.TokenResponse;
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

  private final AuthService authService;

  @PostMapping("/signup")
  public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
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

  @PostMapping("/logout")
  public ResponseEntity<LogoutResponse> logout(@AuthenticationPrincipal Integer userId) {
    return ResponseEntity.ok(authService.logout(userId));
  }

  @PostMapping("/withdrawal")
  public ResponseEntity<WithdrawalResponse> withdraw(
      @AuthenticationPrincipal Integer userId, @Valid @RequestBody WithdrawalRequest request) {
    return ResponseEntity.ok(authService.withdraw(userId, request));
  }

  // 실제로 연결을 맺은 주소만 씀
  //
  // X-Forwarded-For 를 직접 읽지 않는 이유는, 그 헤더를 요청하는 쪽에서 마음대로 지어낼 수 있어서임
  // 헤더를 믿으면 값만 바꿔 가며 보내는 것으로 로그인 잠금을 그대로 통과할 수 있음
  //
  // 프록시 뒤에 둘 때는 server.forward-headers-strategy 를 켜면 됨
  // 그러면 스프링이 프록시가 붙인 헤더를 반영해서 getRemoteAddr() 자체를 바꿔 줌
  // 다만 이건 프록시가 바깥에서 들어온 헤더를 지워 준다는 전제가 있어야 하므로 기본값은 꺼 둠
  private String resolveClientIp(HttpServletRequest request) {
    return request.getRemoteAddr();
  }
}
