package com.nanumi.api.config;

import com.nanumi.api.exception.ErrorCode;
import com.nanumi.api.security.JwtAuthenticationFilter;
import com.nanumi.api.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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

  // JSON 만 돌려주는 API 라서 스크립트도 스타일도 불러올 일이 없음
  private static final String CONTENT_SECURITY_POLICY =
      "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

  // 브라우저 기능을 쓸 일이 없으므로 전부 꺼 둠
  private static final String PERMISSIONS_POLICY =
      "geolocation=(), camera=(), microphone=(), payment=(), usb=()";

  private static final long HSTS_MAX_AGE_SECONDS = 31_536_000L; // 1년

  // H2 콘솔은 개발 프로필에서만 열림
  // 콘솔이 iframe 을 쓰기 때문에 frameOptions 를 풀어야 하는데,
  // 그걸 API 체인에 같이 두면 운영에서도 클릭재킹에 열리므로 체인을 아예 나눔
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  @Profile("dev")
  public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/h2-console/**")
        .csrf(csrf -> csrf.disable())
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

    return http.build();
  }

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE + 1)
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtTokenProvider jwtTokenProvider,
      CorsConfigurationSource corsConfigurationSource)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/auth/signup", "/api/auth/login")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(HSTS_MAX_AGE_SECONDS))
                    .addHeaderWriter(
                        new StaticHeadersWriter("Permissions-Policy", PERMISSIONS_POLICY)))
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                    (request, response, authException) ->
                        writeError(response, ErrorCode.INVALID_TOKEN)))
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtTokenProvider),
            UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.copyOf(corsProperties.getAllowedOrigins()));
    configuration.setAllowedMethods(List.copyOf(corsProperties.getAllowedMethods()));
    configuration.setAllowedHeaders(List.copyOf(corsProperties.getAllowedHeaders()));
    configuration.setAllowCredentials(corsProperties.isAllowCredentials());
    configuration.setMaxAge(corsProperties.getMaxAge());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }

  // 인증에 실패했을 때도 컨트롤러에서 나가는 오류와 같은 모양으로 돌려줌
  // 여기는 MVC 바깥이라 ErrorResponse 를 직접 문자열로 적음
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
