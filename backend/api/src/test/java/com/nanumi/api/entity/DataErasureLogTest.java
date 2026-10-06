package com.nanumi.api.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("개인정보 파기 기록 엔티티")
class DataErasureLogTest {

  private static final LocalDateTime WITHDRAWN_AT = LocalDateTime.of(2026, 10, 1, 12, 0);
  private static final LocalDateTime ERASED_AT = LocalDateTime.of(2026, 10, 31, 4, 0);

  private DataErasureLog newLog() {
    return DataErasureLog.builder()
        .userId(42)
        .withdrawnAt(WITHDRAWN_AT)
        .erasedAt(ERASED_AT)
        .build();
  }

  @Test
  @DisplayName("만든 값이 그대로 담김")
  void 기본값() {
    DataErasureLog log = newLog();

    assertThat(log.getUserId()).isEqualTo(42);
    assertThat(log.getWithdrawnAt()).isEqualTo(WITHDRAWN_AT);
    assertThat(log.getErasedAt()).isEqualTo(ERASED_AT);
  }

  @Test
  @DisplayName("파기 시각은 탈퇴 시각보다 뒤임")
  void 시각_순서() {
    DataErasureLog log = newLog();

    assertThat(log.getErasedAt()).isAfter(log.getWithdrawnAt());
  }

  // 이 표의 존재 이유가 곧 이 테스트의 이유임
  //
  // 약관에 "탈퇴 후 30일이 지나면 파기한다" 고 적어 두었으므로 정말 지웠다는 사실을 보여 줄 수
  // 있어야 함. 그런데 그 기록 자체가 개인정보가 되면 지운 의미가 없어짐.
  // 그래서 회원 번호와 시각만 남기고, 되짚을 수 있는 users/accounts 행은 이미 없는 상태로 둠.
  //
  // 나중에 "누가 지워졌는지 보기 편하게" 이메일이나 닉네임 칸을 더하고 싶어질 수 있는데,
  // 그 순간 이 표가 파기 대상이 되어 버림. 필드가 늘면 이 테스트가 깨지도록 해 둠
  @Test
  @DisplayName("개인을 알아볼 수 있는 칸을 두지 않음")
  void 식별_정보_없음() {
    String[] fieldNames =
        Arrays.stream(DataErasureLog.class.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .map(Field::getName)
            .toArray(String[]::new);

    assertThat(fieldNames)
        .containsExactlyInAnyOrder(
            "id", "userId", "withdrawnAt", "erasedAt", "createdAt", "updatedAt");
  }
}
