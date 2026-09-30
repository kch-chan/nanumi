package com.nanumi.api.config;

import com.nanumi.api.exception.ErrorCode;
import com.nanumi.api.security.ActiveUserGuard;
import com.nanumi.api.security.JwtAuthenticationFilter;
import com.nanumi.api.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  // https 헤더에 들어갈 정책 설정
  // default-src 'none': 모든 외부 리소스 차단
  // frame-ancestors 'none': iframe 에서 열리지 않음 (클릭재킹 방지)
  // base-uri 'none': <base> 태그를 못 씀 (클릭재킹 방지)
  // form-action 'none': form 을 못 씀 (클릭재킹 방지)
  private static final String CONTENT_SECURITY_POLICY =
      "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

  // 브라우저 기능 꺼둠
  // 위치 정보, 카메라, 마이크, 결제, USB 등
  private static final String PERMISSIONS_POLICY =
      "geolocation=(), camera=(), microphone=(), payment=(), usb=()";

  // HSTS(HTTP Strict Transport Security). 앞으로 이 사이트는 HTTPS로만 접속
  private static final long HSTS_MAX_AGE_SECONDS = 31_536_000L; // 1년

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtTokenProvider jwtTokenProvider,
      ActiveUserGuard activeUserGuard,
      CorsConfigurationSource corsConfigurationSource)
      throws Exception {
    // 쿠키나 세션 기반이 아니라 JWT 기반이므로 CSRF(Cross-Site Request Forgery) 방어를 끔
    // 사용자가 로그인한 상태를 악용해서, 사용자가 의도하지 않은 요청을 다른 사이트가 대신 보내게 만드는 공격
    http.csrf(csrf -> csrf.disable())
        // 프런트와 백엔드 포트가 서로 다름. CORS(Cross-Origin Resource Sharing) 정책을 걸어야 함
        // 서로 다른 도메인(Origin) 간에 리소스를 안전하게 공유할 수 있도록 브라우저가 제한을 풀고 제어하는 보안 메커니즘
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        // JWT 기반이므로 세션을 만들지 않음
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // 인증 필요 없는 경로 지정
        .authorizeHttpRequests(
            auth ->
                // Render 가 기동 여부를 확인하는 경로. 상태값만 내려주고 상세는 감춰 둠
                // 하위 경로(/actuator/health/liveness)까지 열어야 함.
                // 배포 플랫폼은 DB 상태가 섞이지 않는 liveness 를 확인 경로로 씀
                auth.requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers("/api/auth/signup", "/api/auth/login", "/api/auth/refresh")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                    .frameOptions(frame -> frame.deny())
                    // 이 사이트에서 다른 사이트로 넘어갈 때 referrer 를 보내지 않음
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                    // https 사용 강제
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(HSTS_MAX_AGE_SECONDS))
                    // 커스텀 헤더 추가. 브라우저 기능 제어
                    .addHeaderWriter(
                        new StaticHeadersWriter("Permissions-Policy", PERMISSIONS_POLICY)))
        // 인증 실패 시 처리
        .exceptionHandling(
            ex ->
                // jwt 토큰이 없거나 잘못된 경우
                ex.authenticationEntryPoint(
                        (request, response, authException) ->
                            writeError(response, ErrorCode.INVALID_TOKEN))
                    // 권한 부족 처리
                    .accessDeniedHandler(
                        (request, response, deniedException) ->
                            writeError(response, ErrorCode.ACCESS_DENIED)))
        // jwt 검증 필터 추가
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtTokenProvider, activeUserGuard), // 너 누구니
            UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  // CORS 구체적인 정책 설정
  @Bean
  public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
    CorsConfiguration configuration = new CorsConfiguration();
    // 어떤 출처에서 요청할 수 있는지, 허용할 http method, 허용할 header, 쿠키 허용 여부
    // setAllowedOrigins 가 아니라 Patterns 를 씀
    // Vercel 은 커밋마다 프리뷰 주소를 새로 만들어서 정확히 일치하는 목록으로는 다 막힘
    // Patterns 는 https://nanumi-*.vercel.app 처럼 와일드카드를 받고, 정확한 주소도 그대로 동작함
    configuration.setAllowedOriginPatterns(List.copyOf(corsProperties.getAllowedOrigins()));
    configuration.setAllowedMethods(List.copyOf(corsProperties.getAllowedMethods()));
    configuration.setAllowedHeaders(List.copyOf(corsProperties.getAllowedHeaders()));
    configuration.setAllowCredentials(corsProperties.isAllowCredentials());
    configuration.setMaxAge(corsProperties.getMaxAge());

    // 어떤 경로에 CORS 정책을 적용할지 지정
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }

  // Spring MVC Controller의 바깥에서 예외가 발생했을 때, JSON 형태로 에러를 내려주기 위한 메서드
  private static void writeError(HttpServletResponse response, ErrorCode errorCode)
      throws IOException {
    response.setStatus(errorCode.getStatus().value());
    response.setContentType("application/json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response
        .getWriter()
        .write(
            "{\"status\":%d,\"message\":\"%s\"}"
                .formatted(errorCode.getStatus().value(), errorCode.getMessage()));
  }
}
