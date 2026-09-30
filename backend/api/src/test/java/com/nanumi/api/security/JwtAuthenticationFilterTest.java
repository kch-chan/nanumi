package com.nanumi.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanumi.api.security.JwtTokenProvider.TokenType;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JWT 인증 필터")
class JwtAuthenticationFilterTest {

  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private ActiveUserGuard activeUserGuard;

  private JwtAuthenticationFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private MockFilterChain chain;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(jwtTokenProvider, activeUserGuard);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    chain = new MockFilterChain();
    // 대부분의 경우는 살아 있는 계정임. 탈퇴 상황은 아래 전용 테스트에서 따로 둠
    when(activeUserGuard.isActive(anyInt())).thenReturn(true);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private Authentication currentAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }

  @Test
  @DisplayName("올바른 액세스 토큰이면 인증 정보를 담음")
  void 인증_성공() throws Exception {
    request.addHeader("Authorization", "Bearer good-token");
    when(jwtTokenProvider.resolveUserId("good-token", TokenType.ACCESS))
        .thenReturn(Optional.of(42));

    filter.doFilter(request, response, chain);

    assertThat(currentAuthentication()).isNotNull();
    assertThat(currentAuthentication().getPrincipal()).isEqualTo(42);
    assertThat(currentAuthentication().getAuthorities()).isEmpty();
  }

  // 토큰이 없다고 여기서 막지 않음. 인증이 필요한 경로인지는 시큐리티가 뒤에서 판단함
  @Test
  @DisplayName("헤더가 없으면 인증 없이 그냥 통과시킴")
  void 헤더_없음() throws Exception {
    when(jwtTokenProvider.resolveUserId(eq(null), any())).thenReturn(Optional.empty());

    filter.doFilter(request, response, chain);

    assertThat(currentAuthentication()).isNull();
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  @DisplayName("Bearer 로 시작하지 않으면 토큰으로 보지 않음")
  void 접두사_없음() throws Exception {
    request.addHeader("Authorization", "Basic abcdef");
    when(jwtTokenProvider.resolveUserId(eq(null), any())).thenReturn(Optional.empty());

    filter.doFilter(request, response, chain);

    verify(jwtTokenProvider).resolveUserId(null, TokenType.ACCESS);
    assertThat(currentAuthentication()).isNull();
  }

  // 리프레시 토큰이 여기서 통과하면 14일짜리 액세스 토큰이 되어 버림
  @Test
  @DisplayName("액세스 토큰이 아니면 인증하지 않음")
  void 리프레시_토큰은_거부() throws Exception {
    request.addHeader("Authorization", "Bearer refresh-token");
    when(jwtTokenProvider.resolveUserId("refresh-token", TokenType.ACCESS))
        .thenReturn(Optional.empty());

    filter.doFilter(request, response, chain);

    assertThat(currentAuthentication()).isNull();
  }

  // 여기서 예외를 던지면 시큐리티의 오류 처리 대신 500 이 나감
  @Test
  @DisplayName("토큰이 망가져 있어도 예외를 던지지 않고 통과시킴")
  void 망가진_토큰() throws Exception {
    request.addHeader("Authorization", "Bearer 이건토큰이아님");
    when(jwtTokenProvider.resolveUserId("이건토큰이아님", TokenType.ACCESS)).thenReturn(Optional.empty());

    filter.doFilter(request, response, chain);

    assertThat(currentAuthentication()).isNull();
    assertThat(chain.getRequest()).isNotNull();
  }

  // 액세스 토큰은 서버가 취소할 수 없음. 이걸 안 보면 탈퇴한 사람이 남은 15분 동안 그대로 통함
  @Test
  @DisplayName("탈퇴한 계정의 토큰이면 서명이 맞아도 인증하지 않음")
  void 탈퇴한_계정은_거부() throws Exception {
    request.addHeader("Authorization", "Bearer good-token");
    when(jwtTokenProvider.resolveUserId("good-token", TokenType.ACCESS))
        .thenReturn(Optional.of(42));
    when(activeUserGuard.isActive(42)).thenReturn(false);

    filter.doFilter(request, response, chain);

    assertThat(currentAuthentication()).isNull();
    // 여기서 막더라도 응답은 시큐리티가 만들어야 하므로 통과는 시킴
    assertThat(chain.getRequest()).isNotNull();
  }

  // 토큰이 없을 때 DB 를 보러 가면 요청마다 헛된 조회가 생김
  @Test
  @DisplayName("토큰이 없으면 계정 상태를 보러 가지 않음")
  void 토큰_없으면_조회_안함() throws Exception {
    when(jwtTokenProvider.resolveUserId(eq(null), any())).thenReturn(Optional.empty());

    filter.doFilter(request, response, chain);

    verify(activeUserGuard, never()).isActive(anyInt());
  }

  @Test
  @DisplayName("인증에 성공해도 다음 필터로 넘김")
  void 다음_필터로_넘김() throws Exception {
    request.addHeader("Authorization", "Bearer good-token");
    when(jwtTokenProvider.resolveUserId("good-token", TokenType.ACCESS)).thenReturn(Optional.of(1));
    FilterChain spyChain = new MockFilterChain();

    filter.doFilter(request, response, spyChain);

    assertThat(((MockFilterChain) spyChain).getRequest()).isSameAs(request);
  }
}
