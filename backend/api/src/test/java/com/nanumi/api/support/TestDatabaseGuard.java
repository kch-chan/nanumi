package com.nanumi.api.support;

import java.util.List;
import java.util.Locale;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.stereotype.Component;

// 테스트가 로컬이 아닌 DB 에 붙는 것을 막음
//
// 왜 필요한가: application-test.yml 은 Flyway 의 clean 을 열어 둠
// (clean-disabled: false, clean-on-validation-error: true).
// 마이그레이션을 고쳤을 때 체크섬이 어긋나 막히는 일을 피하려는 설정이지만,
// TEST_DB_URL 에 운영 주소를 잘못 넣으면 그 DB 의 표를 전부 지워 버림
//
// 그래서 마이그레이션이 돌기 전에 주소를 먼저 확인함.
// FlywayConfigurationCustomizer 는 Flyway 를 만들 때 불리므로 clean/migrate 보다 먼저 돎.
// (@PostConstruct 로 확인하면 이미 지운 뒤에 막을 수 있음)
//
// 원격 DB 로 테스트해야 할 일이 생기면 이 파일을 고쳐야 함. 그 마찰이 이 클래스의 목적임
@Component
public class TestDatabaseGuard implements FlywayConfigurationCustomizer {

  // 이 중 하나를 호스트로 쓰는 주소만 허용함
  private static final List<String> ALLOWED_HOSTS =
      List.of("localhost", "127.0.0.1", "[::1]", "host.docker.internal");

  private final String url;

  TestDatabaseGuard(@Value("${spring.datasource.url}") String url) {
    this.url = url;
  }

  @Override
  public void customize(FluentConfiguration configuration) {
    String lower = url.toLowerCase(Locale.ROOT);
    boolean local = ALLOWED_HOSTS.stream().anyMatch(host -> lower.contains("//" + host));

    if (!local) {
      throw new IllegalStateException(
          """
          테스트 DB 주소가 로컬이 아님: %s

          테스트 설정은 Flyway clean 을 열어 두기 때문에, 이 주소의 표를 전부 지울 수 있음.
          TEST_DB_URL 을 로컬 주소로 바꾸십시오. 허용하는 호스트: %s
          """
              .formatted(url, ALLOWED_HOSTS));
    }
  }
}
