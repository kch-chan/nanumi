package com.nanumi.api.security;

import com.nanumi.api.security.JwtTokenProvider.TokenType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter { // 요청 하나당 한 번만 돌아라

  // 토큰의 약속된 형식
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;

  // HTTP 요청이 들어왔을 때 해당 Filter가 요청을 검사 처리하는 메서드
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    jwtTokenProvider
        .resolveUserId(resolveToken(request), TokenType.ACCESS)
        // Optional 값이 있으면 SecurityContext 에 인증 정보 넣음. 없으면 그냥 통과
        .ifPresent(
            userId ->
                SecurityContextHolder.getContext() // 이 요청은 누가 보내는 가를 담는 용도
                    .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                            userId, null, Collections.emptyList()))); // userId, 자격 증명, 권한 목록

    filterChain.doFilter(request, response); // 다음 필터로 넘김
  }

  // 헤더에서 토큰 꺼내기
  private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    }
    return null;
  }
}
