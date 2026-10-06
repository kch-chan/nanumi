package com.nanumi.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nanumi.api.entity.DataErasureLog;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

// 약관에 "탈퇴 후 30일이 지나면 파기한다" 고 적어 두었으므로 정말 지웠다는 사실을 보여 줄 수 있어야 함.
// 가리킬 users 행이 이미 없으므로 외래키를 걸지 않음 — 그래서 아무 회원 번호나 저장이 됨
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("개인정보 파기 기록 저장소")
class DataErasureLogRepositoryTest {

  @Autowired private DataErasureLogRepository dataErasureLogRepository;

  private DataErasureLog log(int userId) {
    return DataErasureLog.builder()
        .userId(userId)
        .withdrawnAt(LocalDateTime.now().minusDays(30))
        .erasedAt(LocalDateTime.now())
        .build();
  }

  // 지워진 회원의 번호를 가리키므로 users 행이 없어도 저장돼야 함
  @Test
  @DisplayName("회원 행이 없어도 기록을 남길 수 있음")
  void 외래키_없음() {
    DataErasureLog saved = dataErasureLogRepository.saveAndFlush(log(999_999));

    assertThat(saved.getId()).isPositive();
    assertThat(saved.getUserId()).isEqualTo(999_999);
  }

  @Test
  @DisplayName("회원 번호로 기록 유무를 확인함")
  void 기록_확인() {
    dataErasureLogRepository.save(log(1));

    assertThat(dataErasureLogRepository.existsByUserId(1)).isTrue();
    assertThat(dataErasureLogRepository.existsByUserId(2)).isFalse();
  }

  // 파기 배치가 같은 회원을 두 번 처리해도 기록이 두 줄 생기면 안 됨.
  // WithdrawnDataEraser 가 existsByUserId 로 먼저 보고, 그걸 뚫고 들어온 경우는 이 제약이 막음
  @Test
  @DisplayName("같은 회원 번호로 두 줄을 남길 수 없음")
  void 중복_불가() {
    dataErasureLogRepository.save(log(1));

    assertThatThrownBy(() -> dataErasureLogRepository.saveAndFlush(log(1)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("탈퇴 시각과 파기 시각이 그대로 담김")
  void 시각_저장() {
    LocalDateTime withdrawnAt = LocalDateTime.of(2026, 10, 1, 12, 0);
    LocalDateTime erasedAt = LocalDateTime.of(2026, 10, 31, 4, 0);

    DataErasureLog saved =
        dataErasureLogRepository.saveAndFlush(
            DataErasureLog.builder().userId(7).withdrawnAt(withdrawnAt).erasedAt(erasedAt).build());

    assertThat(saved.getWithdrawnAt()).isEqualTo(withdrawnAt);
    assertThat(saved.getErasedAt()).isEqualTo(erasedAt);
    assertThat(saved.getCreatedAt()).isNotNull();
  }
}
