package com.nanumi.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nanumi.api.entity.TermsAgreement;
import com.nanumi.api.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

// 동의를 받았다는 사실의 입증 책임이 사업자에게 있어 반드시 남기는 기록임.
// 그래서 "제대로 남는가" 와 "파기할 때 제대로 지워지는가" 를 같은 무게로 봄
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("약관 동의 저장소")
class TermsAgreementRepositoryTest {

  private static final String VERSION = "시행일자 2026년 8월";

  @Autowired private UserRepository userRepository;
  @Autowired private TermsAgreementRepository termsAgreementRepository;

  private User user;

  @BeforeEach
  void setUp() {
    user = userRepository.save(User.builder().nickname("나눔이").aptName("행복아파트").build());
  }

  private TermsAgreement save(User owner, String key, boolean agreed) {
    return termsAgreementRepository.save(
        TermsAgreement.builder()
            .user(owner)
            .termsKey(key)
            .termsVersion(VERSION)
            .agreed(agreed)
            .agreedAt(LocalDateTime.now())
            .build());
  }

  // 가입 1건에 필수 2건 + 선택 1건, 모두 3행이 생김
  @Test
  @DisplayName("회원 번호로 동의 기록을 모두 찾음")
  void 회원_번호로_찾기() {
    save(user, "service", true);
    save(user, "privacy", true);
    save(user, "marketing", false);

    List<TermsAgreement> agreements = termsAgreementRepository.findByUser_Id(user.getId());

    assertThat(agreements).hasSize(3);
    assertThat(agreements)
        .extracting(TermsAgreement::getTermsKey)
        .containsExactlyInAnyOrder("service", "privacy", "marketing");
  }

  // 선택 약관의 거부도 한 줄로 남음
  // "물어봤고 거부했다" 는 사실 자체가 마케팅을 보내지 않았음을 증명하는 기록이기 때문
  @Test
  @DisplayName("거부한 기록도 함께 남음")
  void 거부도_기록됨() {
    save(user, "marketing", false);

    assertThat(termsAgreementRepository.findByUser_Id(user.getId()))
        .singleElement()
        .satisfies(
            agreement -> {
              assertThat(agreement.getTermsKey()).isEqualTo("marketing");
              assertThat(agreement.isAgreed()).isFalse();
            });
  }

  @Test
  @DisplayName("동의한 약관의 판이 함께 남음")
  void 판_기록() {
    save(user, "service", true);

    assertThat(termsAgreementRepository.findByUser_Id(user.getId()))
        .singleElement()
        .extracting(TermsAgreement::getTermsVersion)
        .isEqualTo(VERSION);
  }

  @Test
  @DisplayName("다른 회원의 기록은 섞이지 않음")
  void 회원별_분리() {
    User other = userRepository.save(User.builder().nickname("이웃").aptName("행복아파트").build());
    save(user, "service", true);
    save(other, "service", true);

    assertThat(termsAgreementRepository.findByUser_Id(user.getId())).hasSize(1);
    assertThat(termsAgreementRepository.findByUser_Id(other.getId())).hasSize(1);
  }

  // 보유 기한(탈퇴 후 30일)이 지나면 동의 기록도 함께 파기함.
  // users 에 ON DELETE CASCADE 가 걸려 있지만 JPA 가 아는 순서로 지워야
  // 영속성 컨텍스트와 DB 가 어긋나지 않으므로 이 메서드로 먼저 지움
  @Test
  @DisplayName("회원 단위로 전부 지움")
  void 회원_단위_삭제() {
    save(user, "service", true);
    save(user, "privacy", true);

    termsAgreementRepository.deleteByUser_Id(user.getId());

    assertThat(termsAgreementRepository.findByUser_Id(user.getId())).isEmpty();
  }

  // 한 회원이 같은 약관에 두 줄을 남기면 어느 것이 최종 의사인지 알 수 없게 됨
  // 재동의(약관 개정 뒤 다시 받는 경우)를 쌓아야 하면 제약을 판까지 넓혀야 함
  @Test
  @DisplayName("한 회원이 같은 약관에 두 줄을 남길 수 없음")
  void 같은_약관_중복_불가() {
    save(user, "service", true);

    assertThatThrownBy(
            () ->
                termsAgreementRepository.saveAndFlush(
                    TermsAgreement.builder()
                        .user(user)
                        .termsKey("service")
                        .termsVersion(VERSION)
                        .agreed(false)
                        .agreedAt(LocalDateTime.now())
                        .build()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
