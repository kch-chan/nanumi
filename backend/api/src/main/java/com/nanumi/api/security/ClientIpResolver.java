package com.nanumi.api.security;

import com.nanumi.api.config.ClientIpProperties;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties.ForwardHeadersStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// 시도 제한이 열쇠로 쓸 "요청을 보낸 사람의 IP" 를 고름
// 어느 값을 믿는지와 그 이유는 ClientIpProperties 주석에 적어 둠
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

  private static final String FORWARDED_FOR = "X-Forwarded-For";

  private final ClientIpProperties clientIpProperties;
  private final ServerProperties serverProperties;

  // forward-headers-strategy 를 켜면 스프링의 ForwardedHeaderFilter 가 먼저 돌면서
  // X-Forwarded-For 를 요청에서 떼어 내고, 대신 getRemoteAddr() 를 "목록의 맨 앞" 값으로 바꿔 놓음.
  // 맨 앞은 요청하는 쪽이 지어낼 수 있는 자리라서, 헤더만 바꿔 가며 시도 제한을 그대로 통과할 수 있음.
  // 그래서 이 프로젝트는 그 기능을 쓰지 않고 아래에서 직접 뒤에서부터 읽음
  @PostConstruct
  void warnOnConflictingSetting() {
    ForwardHeadersStrategy strategy = serverProperties.getForwardHeadersStrategy();
    boolean springEatsTheHeader = strategy != null && strategy != ForwardHeadersStrategy.NONE;

    if (springEatsTheHeader && clientIpProperties.getTrustedProxyCount() > 0) {
      log.warn(
          "server.forward-headers-strategy={} 와 nanumi.security.client-ip.trusted-proxy-count={} 를"
              + " 같이 켜 두면 접속자 IP 를 제대로 읽지 못함."
              + " forward-headers-strategy 는 none 으로 두십시오(FORWARD_HEADERS_STRATEGY 환경 변수 삭제)",
          strategy,
          clientIpProperties.getTrustedProxyCount());
    }
  }

  public String resolve(HttpServletRequest request) {
    int trustedProxyCount = clientIpProperties.getTrustedProxyCount();
    if (trustedProxyCount <= 0) {
      return request.getRemoteAddr();
    }

    String header = request.getHeader(FORWARDED_FOR);
    if (!StringUtils.hasText(header)) {
      // 프록시 뒤에 있다고 적어 뒀는데 헤더가 없음. 설정이 틀렸거나 프록시를 거치지 않은 요청임
      // 지어낼 수 없는 값(실제 연결 주소)으로 돌아감
      return request.getRemoteAddr();
    }

    String[] hops = header.split(",");

    // 뒤에서 trustedProxyCount 번째가 우리 앞 프록시가 적어 준 값임
    // 프록시가 덧붙이지 않고 헤더를 덮어쓰는 방식이면 항목이 하나뿐이라 결과가 같음
    int index = hops.length - trustedProxyCount;
    if (index < 0) {
      // 기대보다 짧음. 믿을 수 있는 구간이 없으니 그중 가장 뒤(우리와 가장 가까운 값)를 씀
      index = hops.length - 1;
    }

    String candidate = hops[index].trim();
    return candidate.isEmpty() ? request.getRemoteAddr() : candidate;
  }
}
