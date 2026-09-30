package com.nanumi.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 요청을 보낸 사람의 IP 를 어디서 읽을지 정함
//
// 우리 앞에 프록시가 몇 대 있는지를 숫자로 적는다.
//   0  프록시 없음. TCP 연결을 맺은 주소를 그대로 씀 (로컬 개발)
//   1  프록시 한 대 뒤에 있음. Render 배포가 여기에 해당함
//   2  CDN 을 하나 더 얹으면 2 가 됨 (예: Cloudflare -> Render -> 앱)
//
// 왜 숫자인가: X-Forwarded-For 는 프록시가 지날 때마다 "직전 상대의 주소"를 뒤에 덧붙이는 목록임.
//
//   X-Forwarded-For: <요청자가 지어낸 값>, ..., <우리 앞 프록시가 붙인 진짜 값>
//
// 앞쪽은 요청하는 쪽이 마음대로 적어 보낼 수 있어 믿을 수 없고, 뒤쪽일수록 우리와 가까워서 믿을 만함.
// 그래서 "뒤에서 몇 번째"를 알아야 하고, 그 숫자가 곧 앞에 둔 프록시 수임
//
// 왜 필요한가: 프록시 뒤에서 이 값을 안 쓰면 getRemoteAddr() 가 프록시의 내부 주소를 돌려줌.
// 그러면 모든 이용자가 같은 열쇠 하나를 쓰게 되어, 아무나 다섯 번 가입한 뒤에는
// 나머지 사람 전부가 429 를 받게 됨. 시도 제한이 방어가 아니라 장애가 됨
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "nanumi.security.client-ip")
public class ClientIpProperties {

  private int trustedProxyCount = 0;
}
