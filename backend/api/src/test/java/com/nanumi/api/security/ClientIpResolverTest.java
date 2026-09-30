package com.nanumi.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanumi.api.config.ClientIpProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.mock.web.MockHttpServletRequest;

@DisplayName("접속자 IP 고르기")
class ClientIpResolverTest {

  private static final String PROXY_IP = "10.0.0.5";
  private static final String REAL_IP = "203.0.113.7";

  private ClientIpResolver resolverWith(int trustedProxyCount) {
    ClientIpProperties properties = new ClientIpProperties();
    properties.setTrustedProxyCount(trustedProxyCount);
    return new ClientIpResolver(properties, new ServerProperties());
  }

  private MockHttpServletRequest requestFrom(String remoteAddr, String forwardedFor) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr(remoteAddr);
    if (forwardedFor != null) {
      request.addHeader("X-Forwarded-For", forwardedFor);
    }
    return request;
  }

  @Nested
  @DisplayName("프록시 없음(로컬)")
  class 프록시_없음 {

    @Test
    @DisplayName("연결을 맺은 주소를 그대로 씀")
    void 연결_주소() {
      String ip = resolverWith(0).resolve(requestFrom(REAL_IP, null));

      assertThat(ip).isEqualTo(REAL_IP);
    }

    // 헤더를 믿으면 값만 바꿔 가며 보내는 것으로 시도 제한을 그대로 통과할 수 있음
    @Test
    @DisplayName("헤더가 와 있어도 보지 않음")
    void 헤더_무시() {
      String ip = resolverWith(0).resolve(requestFrom(REAL_IP, "1.2.3.4"));

      assertThat(ip).isEqualTo(REAL_IP);
    }
  }

  @Nested
  @DisplayName("프록시 한 대 뒤(Render)")
  class 프록시_한_대 {

    // 이걸 못 읽으면 모든 이용자가 프록시 IP 하나를 공유해서, 누군가 다섯 번 가입한 뒤에는
    // 나머지 사람 전부가 429 를 받음
    @Test
    @DisplayName("프록시가 붙인 값을 접속자로 봄")
    void 프록시가_붙인_값() {
      String ip = resolverWith(1).resolve(requestFrom(PROXY_IP, REAL_IP));

      assertThat(ip).isEqualTo(REAL_IP);
    }

    // 목록 앞쪽은 요청하는 쪽이 지어낼 수 있는 자리임
    @Test
    @DisplayName("앞에 지어낸 값을 끼워 보내도 뒤쪽 값을 씀")
    void 지어낸_값_무시() {
      String ip = resolverWith(1).resolve(requestFrom(PROXY_IP, "1.2.3.4, " + REAL_IP));

      assertThat(ip).isEqualTo(REAL_IP);
    }

    @Test
    @DisplayName("여러 개를 지어내 보내도 마지막 값을 씀")
    void 여러_개_지어냄() {
      String ip =
          resolverWith(1).resolve(requestFrom(PROXY_IP, "1.1.1.1, 2.2.2.2, 3.3.3.3, " + REAL_IP));

      assertThat(ip).isEqualTo(REAL_IP);
    }

    // 프록시가 덧붙이지 않고 헤더를 덮어쓰는 방식이면 항목이 하나뿐임
    @Test
    @DisplayName("항목이 하나뿐이면 그 값을 씀")
    void 하나뿐() {
      String ip = resolverWith(1).resolve(requestFrom(PROXY_IP, REAL_IP));

      assertThat(ip).isEqualTo(REAL_IP);
    }

    // 설정이 틀렸거나 프록시를 거치지 않고 직접 들어온 요청임
    @Test
    @DisplayName("헤더가 아예 없으면 연결 주소로 돌아감")
    void 헤더_없음() {
      String ip = resolverWith(1).resolve(requestFrom(PROXY_IP, null));

      assertThat(ip).isEqualTo(PROXY_IP);
    }

    @Test
    @DisplayName("헤더가 비어 있으면 연결 주소로 돌아감")
    void 헤더_빈칸() {
      String ip = resolverWith(1).resolve(requestFrom(PROXY_IP, "   "));

      assertThat(ip).isEqualTo(PROXY_IP);
    }
  }

  @Nested
  @DisplayName("프록시 두 대 뒤(CDN 을 더 얹은 경우)")
  class 프록시_두_대 {

    // Client -> CDN(접속자 IP 를 붙임) -> Render(CDN IP 를 붙임) -> 앱
    @Test
    @DisplayName("뒤에서 두 번째 값을 접속자로 봄")
    void 뒤에서_두_번째() {
      String ip = resolverWith(2).resolve(requestFrom(PROXY_IP, REAL_IP + ", 198.51.100.9"));

      assertThat(ip).isEqualTo(REAL_IP);
    }

    // 기대보다 짧음. 설정이 틀렸거나 프록시 하나가 헤더를 덮어쓴 것임
    // 이때 앞쪽(지어낼 수 있는 자리)을 쓰면 안 되므로 그중 가장 뒤를 씀
    @Test
    @DisplayName("항목이 기대보다 적으면 가장 뒤 값을 씀")
    void 목록이_짧음() {
      String ip = resolverWith(2).resolve(requestFrom(PROXY_IP, REAL_IP));

      assertThat(ip).isEqualTo(REAL_IP);
    }
  }
}
