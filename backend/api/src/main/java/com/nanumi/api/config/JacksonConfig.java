package com.nanumi.api.config;

import com.nanumi.api.security.xss.SanitizingStringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

// 요청 본문의 문자열을 공통으로 다듬도록 Jackson 에 끼워 넣음
// JacksonModule 타입의 빈은 스프링 부트가 알아서 JsonMapper 에 등록해 줌
@Configuration
public class JacksonConfig {

  @Bean
  public JacksonModule sanitizingStringModule() {
    SimpleModule module = new SimpleModule("nanumi-sanitizing-string");
    module.addDeserializer(String.class, new SanitizingStringDeserializer());
    return module;
  }
}
